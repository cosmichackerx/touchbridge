using System.Windows;
using System.Windows.Interop;

namespace TouchBridge.Desktop.Interop;

public static class ClickThrough
{
    public static void Enable(Window window)
    {
        var hwnd = new WindowInteropHelper(window).Handle;
        if (hwnd == IntPtr.Zero) return;

        var exStyle = NativeMethods.GetWindowLong(hwnd, NativeMethods.GwlExStyle);
        var newStyle = (IntPtr)((exStyle.ToInt64()
            | NativeMethods.WsExTransparent
            | NativeMethods.WsExLayered
            | NativeMethods.WsExToolWindow) & ~0);
        NativeMethods.SetWindowLong(hwnd, NativeMethods.GwlExStyle, newStyle);
    }

    public static void Disable(Window window)
    {
        var hwnd = new WindowInteropHelper(window).Handle;
        if (hwnd == IntPtr.Zero) return;

        var exStyle = NativeMethods.GetWindowLong(hwnd, NativeMethods.GwlExStyle);
        var newStyle = (IntPtr)(exStyle.ToInt64()
            & ~NativeMethods.WsExTransparent);
        NativeMethods.SetWindowLong(hwnd, NativeMethods.GwlExStyle, newStyle);
    }
}
