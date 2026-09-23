import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../data/admin_repository.dart';

class AdminUsersScreen extends ConsumerStatefulWidget {
  const AdminUsersScreen({super.key});

  @override
  ConsumerState<AdminUsersScreen> createState() => _AdminUsersScreenState();
}

class _AdminUsersScreenState extends ConsumerState<AdminUsersScreen> {
  List<AdminUserItem> _users = [];
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() => _loading = true);
    try {
      final users = await ref.read(adminRepositoryProvider).listUsers();
      if (mounted) setState(() => _users = users);
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _toggleLock(AdminUserItem user) async {
    await ref.read(adminRepositoryProvider).updateUser(user.id, locked: !user.locked);
    _load();
  }

  Future<void> _toggleRole(AdminUserItem user) async {
    final newRole = user.roles.contains('ROLE_ADMIN') ? 'ROLE_USER' : 'ROLE_ADMIN';
    await ref.read(adminRepositoryProvider).updateUser(user.id, role: newRole);
    _load();
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) return const Center(child: CircularProgressIndicator());
    return RefreshIndicator(
      onRefresh: _load,
      child: ListView.separated(
        padding: const EdgeInsets.all(16),
        itemCount: _users.length,
        separatorBuilder: (_, __) => const Divider(height: 1),
        itemBuilder: (context, i) {
          final user = _users[i];
          final isAdmin = user.roles.contains('ROLE_ADMIN');
          return ListTile(
            leading: CircleAvatar(child: Text(user.username[0].toUpperCase())),
            title: Text(user.username),
            subtitle: Text('${user.email} · ${user.uploadCount} uploads'),
            trailing: Wrap(
              spacing: 4,
              crossAxisAlignment: WrapCrossAlignment.center,
              children: [
                Chip(
                  label: Text(isAdmin ? 'Admin' : 'User', style: const TextStyle(fontSize: 11)),
                  visualDensity: VisualDensity.compact,
                  padding: EdgeInsets.zero,
                ),
                IconButton(
                  tooltip: isAdmin ? 'Demote to user' : 'Promote to admin',
                  icon: Icon(isAdmin ? Icons.remove_moderator_outlined : Icons.admin_panel_settings_outlined),
                  onPressed: () => _toggleRole(user),
                ),
                IconButton(
                  tooltip: user.locked ? 'Unlock account' : 'Lock account',
                  icon: Icon(user.locked ? Icons.lock : Icons.lock_open,
                      color: user.locked ? Theme.of(context).colorScheme.error : null),
                  onPressed: () => _toggleLock(user),
                ),
              ],
            ),
          );
        },
      ),
    );
  }
}
