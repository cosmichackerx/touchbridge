using System.Drawing;
using System.Drawing.Drawing2D;
using System.Runtime.InteropServices;
using System.Windows.Forms;

namespace TouchBridge.Desktop.Interop;

/// <summary>
/// System-tray icon shown when the top bar is minimized. Double-click or "Show bar"
/// restores the overlay; "Exit" shuts the app down.
/// </summary>
public sealed class TrayIconManager : IDisposable
{
    private readonly NotifyIcon _notifyIcon;
    private readonly Icon _icon;
    private bool _disposed;

    public TrayIconManager(Action onShow, Action onExit)
    {
        _icon = CreateIcon();
        _notifyIcon = new NotifyIcon
        {
            Icon = _icon,
            Text = "TouchBridge",
            Visible = false
        };

        _notifyIcon.DoubleClick += (_, _) => onShow();

        var menu = new ContextMenuStrip();
        menu.Items.Add("Show bar", null, (_, _) => onShow());
        menu.Items.Add(new ToolStripSeparator());
        menu.Items.Add("Exit", null, (_, _) => onExit());
        _notifyIcon.ContextMenuStrip = menu;
    }

    public void Show() => _notifyIcon.Visible = true;

    public void Hide() => _notifyIcon.Visible = false;

    public void Dispose()
    {
        if (_disposed) return;
        _disposed = true;
        _notifyIcon.Visible = false;
        _notifyIcon.Dispose();
        _icon.Dispose();
    }

    private static Icon CreateIcon()
    {
        using var bmp = new Bitmap(32, 32, System.Drawing.Imaging.PixelFormat.Format32bppArgb);
        using (var g = Graphics.FromImage(bmp))
        {
            g.SmoothingMode = SmoothingMode.AntiAlias;
            g.Clear(Color.Transparent);
            using var brush = new SolidBrush(Color.FromArgb(79, 142, 247));
            g.FillEllipse(brush, 2, 2, 28, 28);
            using var font = new Font("Segoe UI", 8f, FontStyle.Bold, GraphicsUnit.Point);
            using var textBrush = new SolidBrush(Color.White);
            var size = g.MeasureString("TB", font);
            g.DrawString("TB", font, textBrush, (32f - size.Width) / 2f, (32f - size.Height) / 2f);
        }

        var handle = bmp.GetHicon();
        try
        {
            using var tmp = Icon.FromHandle(handle);
            return (Icon)tmp.Clone();
        }
        finally
        {
            DestroyIcon(handle);
        }
    }

    [DllImport("user32.dll", SetLastError = true)]
    private static extern bool DestroyIcon(IntPtr hIcon);
}
