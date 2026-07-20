using System.Net;
using System.Net.NetworkInformation;
using System.Net.Sockets;

namespace TouchBridge.Desktop.Net;

/// <summary>Helpers for classifying NICs (USB tether vs LAN) for Online/Offline link modes.</summary>
internal static class NetworkEndpoints
{
    public static bool IsUsbTetherInterface(NetworkInterface ni)
    {
        var desc = ni.Description ?? "";
        var name = ni.Name ?? "";
        return desc.Contains("RNDIS", StringComparison.OrdinalIgnoreCase)
               || desc.Contains("Remote NDIS", StringComparison.OrdinalIgnoreCase)
               || desc.Contains("Android USB", StringComparison.OrdinalIgnoreCase)
               || (desc.Contains("Android", StringComparison.OrdinalIgnoreCase)
                   && desc.Contains("Ethernet", StringComparison.OrdinalIgnoreCase))
               || name.Contains("Remote NDIS", StringComparison.OrdinalIgnoreCase);
    }

    public static bool IsVirtualOrIgnoredInterface(NetworkInterface ni)
    {
        var desc = ni.Description ?? "";
        var name = ni.Name ?? "";
        return desc.Contains("Hyper-V", StringComparison.OrdinalIgnoreCase)
               || desc.Contains("Virtual", StringComparison.OrdinalIgnoreCase)
               || desc.Contains("VMware", StringComparison.OrdinalIgnoreCase)
               || desc.Contains("VirtualBox", StringComparison.OrdinalIgnoreCase)
               || name.Contains("vEthernet", StringComparison.OrdinalIgnoreCase)
               || name.StartsWith("Loopback", StringComparison.OrdinalIgnoreCase);
    }

    public static bool IsUsbTetherIp(string ip)
    {
        foreach (var ni in NetworkInterface.GetAllNetworkInterfaces())
        {
            if (ni.OperationalStatus != OperationalStatus.Up) continue;
            if (!IsUsbTetherInterface(ni)) continue;
            foreach (var ua in ni.GetIPProperties().UnicastAddresses)
            {
                if (ua.Address.AddressFamily == AddressFamily.InterNetwork
                    && ua.Address.ToString() == ip)
                    return true;
            }
        }
        return false;
    }

    /// <summary>First usable IPv4 on an Android USB / RNDIS adapter, if any.</summary>
    public static string? GetUsbTetherIpv4()
    {
        foreach (var ni in NetworkInterface.GetAllNetworkInterfaces())
        {
            if (ni.OperationalStatus != OperationalStatus.Up) continue;
            if (!IsUsbTetherInterface(ni)) continue;
            foreach (var ua in ni.GetIPProperties().UnicastAddresses)
            {
                if (ua.Address.AddressFamily != AddressFamily.InterNetwork) continue;
                var ip = ua.Address.ToString();
                if (IPAddress.IsLoopback(ua.Address)) continue;
                if (ip.StartsWith("169.254.", StringComparison.Ordinal)) continue;
                return ip;
            }
        }
        return null;
    }

    public static IEnumerable<(NetworkInterface Ni, string Ip)> EnumerateIpv4()
    {
        foreach (var ni in NetworkInterface.GetAllNetworkInterfaces())
        {
            if (ni.OperationalStatus != OperationalStatus.Up) continue;
            if (ni.NetworkInterfaceType is NetworkInterfaceType.Loopback) continue;
            if (IsVirtualOrIgnoredInterface(ni)) continue;
            foreach (var ua in ni.GetIPProperties().UnicastAddresses)
            {
                if (ua.Address.AddressFamily != AddressFamily.InterNetwork) continue;
                var ip = ua.Address.ToString();
                if (IPAddress.IsLoopback(ua.Address)) continue;
                if (ip.StartsWith("169.254.", StringComparison.Ordinal)) continue;
                yield return (ni, ip);
            }
        }
    }

    public static bool IsSameSubnet(IPAddress local, IPAddress remote, IPAddress mask)
    {
        var l = local.GetAddressBytes();
        var r = remote.GetAddressBytes();
        var m = mask.GetAddressBytes();
        if (l.Length != r.Length || l.Length != m.Length) return false;
        for (var i = 0; i < l.Length; i++)
        {
            if ((l[i] & m[i]) != (r[i] & m[i])) return false;
        }
        return true;
    }
}
