using System.Linq;
using System.Windows;
using TouchBridge.Desktop.Core;

namespace TouchBridge.Desktop.Views;

public partial class SettingsWindow : Window
{
    public string NgrokDomain { get; private set; }
    public string Password { get; private set; }

    /// <summary>Wire name of the selected keyboard skin.</summary>
    public string KeyboardTheme { get; private set; }

    private sealed record ThemeItem(KeyboardTheme Id, string Label);

    public SettingsWindow(string ngrokDomain, string password, string keyboardTheme)
    {
        InitializeComponent();
        NgrokDomain = ngrokDomain;
        Password = password;
        KeyboardTheme = keyboardTheme;

        DomainBox.Text = ngrokDomain;
        PasswordBox.Text = password;

        var items = KeyboardThemes.All.Select(t => new ThemeItem(t.Id, t.Label)).ToList();
        ThemeBox.ItemsSource = items;
        var current = Core.KeyboardThemes.FromWire(keyboardTheme);
        ThemeBox.SelectedItem = items.FirstOrDefault(i => i.Id == current) ?? items[0];
    }

    private void OnSave(object sender, RoutedEventArgs e)
    {
        NgrokDomain = DomainBox.Text.Trim();
        Password = PasswordBox.Text.Trim();
        if (ThemeBox.SelectedItem is ThemeItem theme)
            KeyboardTheme = theme.Id.Wire();
        DialogResult = true;
        Close();
    }

    private void OnCancel(object sender, RoutedEventArgs e)
    {
        DialogResult = false;
        Close();
    }
}
