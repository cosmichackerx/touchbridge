namespace TouchBridge.Desktop.Input;

public static class PointerAccel
{
    public static (int dx, int dy) Apply(float dx, float dy, float sensitivity)
    {
        var magnitude = MathF.Sqrt(dx * dx + dy * dy);
        if (magnitude < 0.5f) return (0, 0);

        var scale = sensitivity * (1f + magnitude * 0.08f);
        return ((int)MathF.Round(dx * scale), (int)MathF.Round(dy * scale));
    }
}
