import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../data/content_models.dart';
import '../data/content_repository.dart';

class NotificationsScreen extends ConsumerStatefulWidget {
  const NotificationsScreen({super.key});

  @override
  ConsumerState<NotificationsScreen> createState() => _NotificationsScreenState();
}

class _NotificationsScreenState extends ConsumerState<NotificationsScreen> {
  PageResult<NotificationItem> _result = PageResult.empty<NotificationItem>();
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() => _loading = true);
    try {
      final result = await ref.read(contentRepositoryProvider).listNotifications();
      if (mounted) setState(() => _result = result);
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _markRead(NotificationItem n) async {
    if (n.read) return;
    await ref.read(contentRepositoryProvider).markNotificationRead(n.id);
    _load();
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) return const Center(child: CircularProgressIndicator());
    if (_result.content.isEmpty) {
      return const Center(child: Text('No notifications yet'));
    }
    return RefreshIndicator(
      onRefresh: _load,
      child: ListView.separated(
        padding: const EdgeInsets.all(16),
        itemCount: _result.content.length,
        separatorBuilder: (_, __) => const Divider(height: 1),
        itemBuilder: (context, i) {
          final n = _result.content[i];
          return ListTile(
            leading: Icon(n.read ? Icons.notifications_none : Icons.notifications_active,
                color: n.read ? null : Theme.of(context).colorScheme.primary),
            title: Text(n.title, style: TextStyle(fontWeight: n.read ? FontWeight.normal : FontWeight.bold)),
            subtitle: Text(n.message ?? ''),
            trailing: n.createdAt != null ? Text(DateFormat.MMMd().add_Hm().format(n.createdAt!)) : null,
            onTap: () => _markRead(n),
          );
        },
      ),
    );
  }
}
