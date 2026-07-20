using System.Net;
using System.Net.NetworkInformation;
using System.Net.Sockets;
using System.Text;
using System.Text.Json;
using TouchBridge.Desktop.Core;

namespace TouchBridge.Desktop.Net;

public sealed class DiscoveryResponder : IDisposable
{
    private readonly AppState _state;
    private readonly UdpClient _client;
    private readonly CancellationTokenSource _cts = new();
    private readonly Task _listenTask;

    public DiscoveryResponder(AppState state)
    {
        _state = state;
        _client = new UdpClient(ProtocolConstants.DiscoveryPort);
        _client.EnableBroadcast = true;
        _listenTask = Task.Run(ListenLoop);
    }

    public void Dispose()
    {
        _cts.Cancel();
        _client.Close();
        try { _listenTask.Wait(1000); } catch { /* shutting down */ }
        _cts.Dispose();
    }

    private async Task ListenLoop()
    {
        while (!_cts.IsCancellationRequested)
        {
            try
            {
                var result = await _client.ReceiveAsync(_cts.Token);
                var text = Encoding.UTF8.GetString(result.Buffer);
                using var doc = JsonDocument.Parse(text);
                var root = doc.RootElement;

                if (!root.TryGetProperty("magic", out var magic) ||
                    magic.GetString() != ProtocolConstants.Magic)
                    continue;
                if (!root.TryGetProperty("t", out var type) ||
                    type.GetString() != "discover")
                    continue;

                var hosts = GetAnnounceHosts(result.RemoteEndPoint.Address, _state.LinkMode);
                foreach (var (localIp, link) in hosts)
                {
                    var reply = JsonSerializer.Serialize(new
                    {
                        magic = ProtocolConstants.Magic,
                        t = "announce",
                        v = ProtocolConstants.Version,
                        name = _state.DeviceName,
                        host = localIp,
                        port = ProtocolConstants.ControlPort,
                        requiresPin = true,
                        link
                    });

                    var bytes = Encoding.UTF8.GetBytes(reply);
                    await _client.SendAsync(bytes, result.RemoteEndPoint, _cts.Token);
                }
            }
            catch (OperationCanceledException) { break; }
            catch { /* ignore malformed packets */ }
        }
    }

    /// <summary>
    /// Offline → USB tether IPs only. Online → LAN/Wi‑Fi (non‑USB) IPs only.
    /// Same-subnet matches are listed first.
    /// </summary>
    private static List<(string Ip, string Link)> GetAnnounceHosts(IPAddress remote, NetworkLinkMode mode)
    {
        var sameSubnet = new List<(string Ip, string Link)>();
        var others = new List<(string Ip, string Link)>();

        foreach (var (ni, ip) in NetworkEndpoints.EnumerateIpv4())
        {
            var isUsb = NetworkEndpoints.IsUsbTetherInterface(ni);
            if (mode == NetworkLinkMode.Offline && !isUsb) continue;
            if (mode == NetworkLinkMode.Online && isUsb) continue;

            var link = isUsb ? "usb" : "lan";
            UnicastIPAddressInformation? matchUa = null;
            foreach (var ua in ni.GetIPProperties().UnicastAddresses)
            {
                if (ua.Address.ToString() == ip)
                {
                    matchUa = ua;
                    break;
                }
            }

            if (matchUa?.IPv4Mask is not null
                && NetworkEndpoints.IsSameSubnet(matchUa.Address, remote, matchUa.IPv4Mask))
                sameSubnet.Add((ip, link));
            else
                others.Add((ip, link));
        }

        var chosen = sameSubnet.Count > 0 ? sameSubnet : others;
        if (chosen.Count > 0) return chosen.Distinct().ToList();

        // Offline with no USB adapter yet — announce nothing (phone keeps waiting).
        if (mode == NetworkLinkMode.Offline) return [];

        return [(GetDefaultRouteIp(), "lan")];
    }

    private static string GetDefaultRouteIp()
    {
        try
        {
            using var socket = new Socket(AddressFamily.InterNetwork, SocketType.Dgram, 0);
            socket.Connect("8.8.8.8", 65530);
            var ep = socket.LocalEndPoint as IPEndPoint;
            return ep?.Address.ToString() ?? "127.0.0.1";
        }
        catch
        {
            return "127.0.0.1";
        }
    }
}
