using System.Security.Cryptography;
using System.Text;

namespace TouchBridge.Desktop.Crypto;

/// <summary>
/// End-to-end encryption for the control channel. A 256-bit key is derived from the shared
/// password with PBKDF2-HMAC-SHA256; every control frame is sealed with AES-256-GCM so the
/// ngrok relay (or any intermediary) only ever sees ciphertext.
///
/// Envelope layout (binary WebSocket message):
///   [0]      kind byte (0x01 = JSON/text payload, 0x02 = raw binary opcode frame)
///   [1..13)  12-byte random nonce
///   [13..]   ciphertext followed by the 16-byte GCM tag
/// The kind byte is bound as additional authenticated data.
/// </summary>
public static class SecureChannel
{
    public const int SaltSize = 16;
    public const int NonceSize = 12;
    public const int TagSize = 16;
    public const int KeySize = 32;
    public const int Iterations = 100_000;

    public const byte KindText = 0x01;
    public const byte KindBinary = 0x02;
    public const byte KindScreen = 0x03;

    private static readonly byte[] AuthPlaintext = "TB-AUTH-v1"u8.ToArray();
    private static readonly byte[] AuthAad = "tb-auth"u8.ToArray();

    public static byte[] DeriveKey(string password, byte[] salt) =>
        Rfc2898DeriveBytes.Pbkdf2(
            Encoding.UTF8.GetBytes(password ?? ""),
            salt,
            Iterations,
            HashAlgorithmName.SHA256,
            KeySize);

    public static byte[] RandomBytes(int length) => RandomNumberGenerator.GetBytes(length);

    /// <summary>Seals a payload into an encrypted envelope.</summary>
    public static byte[] Seal(byte[] key, byte kind, ReadOnlySpan<byte> plaintext)
    {
        var nonce = RandomBytes(NonceSize);
        var cipher = new byte[plaintext.Length];
        var tag = new byte[TagSize];

        using var gcm = new AesGcm(key, TagSize);
        gcm.Encrypt(nonce, plaintext, cipher, tag, [kind]);

        var output = new byte[1 + NonceSize + cipher.Length + TagSize];
        output[0] = kind;
        nonce.CopyTo(output, 1);
        cipher.CopyTo(output, 1 + NonceSize);
        tag.CopyTo(output, 1 + NonceSize + cipher.Length);
        return output;
    }

    public static bool TryOpen(byte[] key, ReadOnlySpan<byte> envelope, out byte kind, out byte[] plaintext)
    {
        kind = 0;
        plaintext = [];
        if (envelope.Length < 1 + NonceSize + TagSize) return false;

        kind = envelope[0];
        var nonce = envelope.Slice(1, NonceSize);
        var cipherLen = envelope.Length - 1 - NonceSize - TagSize;
        var cipher = envelope.Slice(1 + NonceSize, cipherLen);
        var tag = envelope.Slice(1 + NonceSize + cipherLen, TagSize);

        var output = new byte[cipherLen];
        try
        {
            using var gcm = new AesGcm(key, TagSize);
            gcm.Decrypt(nonce, cipher, tag, output, [kind]);
            plaintext = output;
            return true;
        }
        catch (CryptographicException)
        {
            return false;
        }
    }

    /// <summary>Builds the client auth proof: encrypt a known token with the derived key.</summary>
    public static (byte[] nonce, byte[] cipherTag) MakeAuthProof(byte[] key)
    {
        var nonce = RandomBytes(NonceSize);
        var cipher = new byte[AuthPlaintext.Length];
        var tag = new byte[TagSize];

        using var gcm = new AesGcm(key, TagSize);
        gcm.Encrypt(nonce, AuthPlaintext, cipher, tag, AuthAad);

        var cipherTag = new byte[cipher.Length + TagSize];
        cipher.CopyTo(cipherTag, 0);
        tag.CopyTo(cipherTag, cipher.Length);
        return (nonce, cipherTag);
    }

    /// <summary>Verifies the client auth proof; succeeds only when the derived keys match.</summary>
    public static bool VerifyAuthProof(byte[] key, byte[] nonce, byte[] cipherTag)
    {
        if (nonce.Length != NonceSize || cipherTag.Length < TagSize) return false;

        var cipherLen = cipherTag.Length - TagSize;
        var cipher = cipherTag.AsSpan(0, cipherLen);
        var tag = cipherTag.AsSpan(cipherLen, TagSize);
        var output = new byte[cipherLen];

        try
        {
            using var gcm = new AesGcm(key, TagSize);
            gcm.Decrypt(nonce, cipher, tag, output, AuthAad);
            return output.AsSpan().SequenceEqual(AuthPlaintext);
        }
        catch (CryptographicException)
        {
            return false;
        }
    }
}
