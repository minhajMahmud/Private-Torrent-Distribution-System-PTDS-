import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../content/data/content_models.dart';
import '../data/admin_repository.dart';

class AdminModerationScreen extends ConsumerStatefulWidget {
  const AdminModerationScreen({super.key});

  @override
  ConsumerState<AdminModerationScreen> createState() => _AdminModerationScreenState();
}

class _AdminModerationScreenState extends ConsumerState<AdminModerationScreen> {
  PageResult<FileItem> _result = PageResult.empty<FileItem>();
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() => _loading = true);
    try {
      final result = await ref.read(adminRepositoryProvider).moderationQueue();
      if (mounted) setState(() => _result = result);
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _approve(FileItem file) async {
    await ref.read(adminRepositoryProvider).moderate(file.id, approve: true);
    _load();
  }

  Future<void> _reject(FileItem file) async {
    final reasonController = TextEditingController();
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Reject file'),
        content: TextField(
          controller: reasonController,
          decoration: const InputDecoration(labelText: 'Reason (shown to the uploader)'),
          maxLines: 3,
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Cancel')),
          FilledButton(onPressed: () => Navigator.pop(context, true), child: const Text('Reject')),
        ],
      ),
    );
    if (confirmed == true) {
      await ref.read(adminRepositoryProvider).moderate(file.id, approve: false, rejectionReason: reasonController.text);
      _load();
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) return const Center(child: CircularProgressIndicator());
    if (_result.content.isEmpty) {
      return const Center(child: Text('Nothing pending moderation 🎉'));
    }
    return RefreshIndicator(
      onRefresh: _load,
      child: ListView.separated(
        padding: const EdgeInsets.all(16),
        itemCount: _result.content.length,
        separatorBuilder: (_, __) => const Divider(height: 1),
        itemBuilder: (context, i) {
          final file = _result.content[i];
          return ListTile(
            title: Text(file.title),
            subtitle: Text('${file.uploaderUsername} · ${file.readableSize} · ${file.originalName}'),
            trailing: Wrap(
              spacing: 8,
              children: [
                IconButton(
                  tooltip: 'Approve',
                  icon: const Icon(Icons.check_circle, color: Colors.green),
                  onPressed: () => _approve(file),
                ),
                IconButton(
                  tooltip: 'Reject',
                  icon: Icon(Icons.cancel, color: Theme.of(context).colorScheme.error),
                  onPressed: () => _reject(file),
                ),
              ],
            ),
          );
        },
      ),
    );
  }
}
