import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/theme/theme_provider.dart';
import '../../admin/presentation/admin_shell_screen.dart';
import '../../auth/application/auth_controller.dart';
import '../../content/presentation/file_browse_screen.dart';
import '../../content/presentation/file_upload_screen.dart';
import '../../content/presentation/notifications_screen.dart';
import '../../profile/presentation/profile_screen.dart';

/// Authenticated shell: responsive nav rail (desktop) / bottom nav (mobile)
/// around the real feature screens (browse, upload, favorites, profile —
/// plus an Admin tab for admins).
class DashboardShell extends ConsumerStatefulWidget {
  const DashboardShell({super.key});

  @override
  ConsumerState<DashboardShell> createState() => _DashboardShellState();
}

class _DashboardShellState extends ConsumerState<DashboardShell> {
  int _selected = 0;

  static const _baseDestinations = [
    (icon: Icons.home_outlined, selectedIcon: Icons.home, label: 'Home'),
    (icon: Icons.upload_file_outlined, selectedIcon: Icons.upload_file, label: 'Upload'),
    (icon: Icons.favorite_border, selectedIcon: Icons.favorite, label: 'Favorites'),
    (icon: Icons.notifications_outlined, selectedIcon: Icons.notifications, label: 'Notifications'),
    (icon: Icons.person_outline, selectedIcon: Icons.person, label: 'Profile'),
  ];
  static const _adminDestination =
      (icon: Icons.admin_panel_settings_outlined, selectedIcon: Icons.admin_panel_settings, label: 'Admin');

  @override
  Widget build(BuildContext context) {
    final isWide = MediaQuery.sizeOf(context).width >= 900;
    final themeMode = ref.watch(themeModeProvider);
    final authState = ref.watch(authControllerProvider);
    final username = authState is AuthAuthenticated ? authState.user.username : 'User';
    final isAdmin = authState is AuthAuthenticated && authState.user.isAdmin;

    final destinations = isAdmin ? [..._baseDestinations, _adminDestination] : _baseDestinations;
    final selected = _selected > destinations.length - 1 ? destinations.length - 1 : _selected;

    final screens = [
      const FileBrowseScreen(),
      const FileUploadScreen(),
      const FileBrowseScreen(mode: FileBrowseMode.favorites),
      const NotificationsScreen(),
      const ProfileScreen(),
      if (isAdmin) const AdminShellScreen(),
    ];

    return Scaffold(
      appBar: AppBar(
        title: Row(
          children: [
            Icon(Icons.hub_outlined, color: Theme.of(context).colorScheme.primary),
            const SizedBox(width: 8),
            const Text('PTDS'),
          ],
        ),
        actions: [
          IconButton(
            tooltip: 'Toggle theme',
            icon: Icon(themeMode == ThemeMode.dark
                ? Icons.light_mode_outlined
                : Icons.dark_mode_outlined),
            onPressed: () => ref.read(themeModeProvider.notifier).toggle(),
          ),
          const SizedBox(width: 8),
          CircleAvatar(
            radius: 16,
            child: Text(username.isNotEmpty ? username[0].toUpperCase() : '?'),
          ),
          const SizedBox(width: 8),
          IconButton(
            tooltip: 'Logout',
            icon: const Icon(Icons.logout),
            onPressed: () async {
              await ref.read(authControllerProvider.notifier).logout();
              if (context.mounted) context.go('/login');
            },
          ),
          const SizedBox(width: 8),
        ],
      ),
      body: Row(
        children: [
          if (isWide)
            NavigationRail(
              selectedIndex: selected,
              onDestinationSelected: (i) => setState(() => _selected = i),
              labelType: NavigationRailLabelType.all,
              destinations: [
                for (final d in destinations)
                  NavigationRailDestination(
                    icon: Icon(d.icon),
                    selectedIcon: Icon(d.selectedIcon),
                    label: Text(d.label),
                  ),
              ],
            ),
          if (isWide) const VerticalDivider(width: 1),
          Expanded(child: screens[selected]),
        ],
      ),
      bottomNavigationBar: isWide
          ? null
          : NavigationBar(
              selectedIndex: selected,
              onDestinationSelected: (i) => setState(() => _selected = i),
              destinations: [
                for (final d in destinations)
                  NavigationDestination(icon: Icon(d.icon), label: d.label),
              ],
            ),
    );
  }
}
