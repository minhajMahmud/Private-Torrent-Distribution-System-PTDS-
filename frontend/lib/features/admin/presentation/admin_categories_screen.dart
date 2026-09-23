import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../content/data/content_models.dart';
import '../../content/data/content_repository.dart';
import '../data/admin_repository.dart';

class AdminCategoriesScreen extends ConsumerStatefulWidget {
  const AdminCategoriesScreen({super.key});

  @override
  ConsumerState<AdminCategoriesScreen> createState() => _AdminCategoriesScreenState();
}

class _AdminCategoriesScreenState extends ConsumerState<AdminCategoriesScreen> {
  List<CategoryItem> _categories = [];
  bool _loading = true;
  final _nameController = TextEditingController();

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() => _loading = true);
    try {
      final categories = await ref.read(contentRepositoryProvider).listCategories();
      if (mounted) setState(() => _categories = categories);
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _create() async {
    final name = _nameController.text.trim();
    if (name.isEmpty) return;
    try {
      await ref.read(adminRepositoryProvider).createCategory(name);
      _nameController.clear();
      _load();
    } catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.toString())));
    }
  }

  Future<void> _delete(CategoryItem category) async {
    await ref.read(adminRepositoryProvider).deleteCategory(category.id);
    _load();
  }

  @override
  void dispose() {
    _nameController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) return const Center(child: CircularProgressIndicator());
    return Padding(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: TextField(
                  controller: _nameController,
                  decoration: const InputDecoration(labelText: 'New category name', isDense: true),
                  onSubmitted: (_) => _create(),
                ),
              ),
              const SizedBox(width: 12),
              FilledButton(onPressed: _create, child: const Text('Add')),
            ],
          ),
          const SizedBox(height: 16),
          Expanded(
            child: RefreshIndicator(
              onRefresh: _load,
              child: ListView.separated(
                itemCount: _categories.length,
                separatorBuilder: (_, __) => const Divider(height: 1),
                itemBuilder: (context, i) {
                  final c = _categories[i];
                  return ListTile(
                    title: Text(c.name),
                    subtitle: Text('${c.fileCount} files · /${c.slug}'),
                    trailing: IconButton(
                      icon: const Icon(Icons.delete_outline),
                      onPressed: () => _delete(c),
                    ),
                  );
                },
              ),
            ),
          ),
        ],
      ),
    );
  }
}
