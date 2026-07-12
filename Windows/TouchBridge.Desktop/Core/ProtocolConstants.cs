namespace TouchBridge.Desktop.Core;

public static class ProtocolConstants
{
    public const string Magic = "TBRDG";
    public const int Version = 1;
    public const int DiscoveryPort = 47832;
    public const int ControlPort = 47831;
    public const string WebSocketPath = "/tb";

    public const byte OpcodeMove = 0x01;
    public const byte OpcodeScroll = 0x02;
}
