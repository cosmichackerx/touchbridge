using System.Diagnostics;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.Drawing.Imaging;
using System.IO;
using System.Runtime.InteropServices;
using TouchBridge.Desktop.Interop;

namespace TouchBridge.Desktop.Capture;

/// <summary>
/// Captures the primary desktop on a background loop, scales each frame down, JPEG-encodes it,
/// and hands the bytes to a callback (which seals + sends them over the encrypted channel).
/// Runs only while <see cref="Start"/> has been called, so it costs nothing when the phone
/// has screen view turned off.
/// </summary>
public sealed class ScreenStreamer : IDisposable
{
    private readonly Func<byte[], Task> _sendFrame;
    private readonly int _targetHeight;
    private readonly long _jpegQuality;
    private readonly int _frameDelayMs;

    private readonly object _gate = new();
    private CancellationTokenSource? _cts;
    private Task? _loop;

    private static readonly ImageCodecInfo JpegCodec =
        ImageCodecInfo.GetImageEncoders().First(c => c.FormatID == ImageFormat.Jpeg.Guid);

    public ScreenStreamer(Func<byte[], Task> sendFrame, int targetHeight = 720, long jpegQuality = 55, int fps = 10)
    {
        _sendFrame = sendFrame;
        _targetHeight = targetHeight;
        _jpegQuality = jpegQuality;
        _frameDelayMs = Math.Max(1, 1000 / Math.Max(1, fps));
    }

    public bool IsRunning
    {
        get { lock (_gate) return _loop is { IsCompleted: false }; }
    }

    public void Start()
    {
        lock (_gate)
        {
            if (_loop is { IsCompleted: false }) return;
            _cts = new CancellationTokenSource();
            _loop = Task.Run(() => CaptureLoop(_cts.Token));
        }
    }

    public void Stop()
    {
        CancellationTokenSource? cts;
        Task? loop;
        lock (_gate)
        {
            cts = _cts;
            loop = _loop;
            _cts = null;
            _loop = null;
        }

        if (cts is null) return;
        try { cts.Cancel(); } catch { /* already gone */ }
        try { loop?.Wait(1000); } catch { /* ending */ }
        cts.Dispose();
    }

    private async Task CaptureLoop(CancellationToken token)
    {
        var sw = Stopwatch.StartNew();
        while (!token.IsCancellationRequested)
        {
            sw.Restart();
            try
            {
                var jpeg = CaptureJpeg();
                if (jpeg is not null)
                    await _sendFrame(jpeg);
            }
            catch (OperationCanceledException) { break; }
            catch { /* transient capture/send failure — try the next frame */ }

            var elapsed = (int)sw.ElapsedMilliseconds;
            var wait = _frameDelayMs - elapsed;
            if (wait > 0)
            {
                try { await Task.Delay(wait, token); }
                catch (OperationCanceledException) { break; }
            }
        }
    }

    private byte[]? CaptureJpeg()
    {
        int sw = NativeMethods.GetSystemMetrics(NativeMethods.SmCxScreen);
        int sh = NativeMethods.GetSystemMetrics(NativeMethods.SmCyScreen);
        if (sw <= 0 || sh <= 0) return null;

        using var full = new Bitmap(sw, sh, PixelFormat.Format24bppRgb);
        using (var g = Graphics.FromImage(full))
        {
            g.CopyFromScreen(0, 0, 0, 0, new Size(sw, sh), CopyPixelOperation.SourceCopy);
            DrawCursor(g);
        }

        // Scale to the target height, preserving aspect ratio; never upscale.
        int th = Math.Min(_targetHeight, sh);
        int tw = (int)Math.Round(sw * (th / (double)sh));
        if (tw < 1) tw = 1;

        Bitmap frame;
        bool disposeFrame = false;
        if (tw == sw && th == sh)
        {
            frame = full;
        }
        else
        {
            frame = new Bitmap(tw, th, PixelFormat.Format24bppRgb);
            disposeFrame = true;
            using var g2 = Graphics.FromImage(frame);
            g2.InterpolationMode = InterpolationMode.Bilinear;
            g2.PixelOffsetMode = PixelOffsetMode.Half;
            g2.DrawImage(full, 0, 0, tw, th);
        }

        try
        {
            using var ms = new MemoryStream();
            using var parms = new EncoderParameters(1);
            parms.Param[0] = new EncoderParameter(Encoder.Quality, _jpegQuality);
            frame.Save(ms, JpegCodec, parms);
            return ms.ToArray();
        }
        finally
        {
            if (disposeFrame) frame.Dispose();
        }
    }

    private static void DrawCursor(Graphics g)
    {
        var ci = new NativeMethods.CURSORINFO { cbSize = Marshal.SizeOf<NativeMethods.CURSORINFO>() };
        if (!NativeMethods.GetCursorInfo(ref ci) || ci.flags != NativeMethods.CursorShowing || ci.hCursor == IntPtr.Zero)
            return;

        int hotX = 0, hotY = 0;
        if (NativeMethods.GetIconInfo(ci.hCursor, out var iconInfo))
        {
            hotX = iconInfo.xHotspot;
            hotY = iconInfo.yHotspot;
            if (iconInfo.hbmMask != IntPtr.Zero) NativeMethods.DeleteObject(iconInfo.hbmMask);
            if (iconInfo.hbmColor != IntPtr.Zero) NativeMethods.DeleteObject(iconInfo.hbmColor);
        }

        var hdc = g.GetHdc();
        try
        {
            NativeMethods.DrawIconEx(hdc, ci.ptScreenPos.x - hotX, ci.ptScreenPos.y - hotY,
                ci.hCursor, 0, 0, 0, IntPtr.Zero, NativeMethods.DiNormal);
        }
        finally
        {
            g.ReleaseHdc(hdc);
        }
    }

    public void Dispose() => Stop();
}
