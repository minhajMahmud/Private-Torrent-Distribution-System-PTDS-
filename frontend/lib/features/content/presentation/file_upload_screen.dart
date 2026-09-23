import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../data/content_models.dart';
import '../data/content_repository.dart';

class FileUploadScreen extends ConsumerStatefulWidget {
  const FileUploadScreen({super.key});

  @override
  ConsumerState<FileUploadScreen> createState() => _FileUploadScreenState();
}

class _FileUploadScreenState extends ConsumerState<FileUploadScreen> {
  final _formKey = GlobalKey<FormState>();
  final _titleController = TextEditingController();
  final _descriptionController = TextEditingController();
  final _tagsController = TextEditingController();
  int? _categoryId;
  List<CategoryItem> _categories = [];
  PlatformFile? _picked;
  bool _submitting = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    ref.read(contentRepositoryProvider).listCategories().then((c) {
      if (mounted) setState(() => _categories = c);
    }).catchError((_) {});
  }

  Future<void> _pickFile() async {
    final result = await FilePicker.platform.pickFiles(withData: true);
    if (result != null && result.files.isNotEmpty) {
      setState(() => _picked = result.files.first);
    }
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    if (_picked == null || _picked!.bytes == null) {
      setState(() => _error = 'Please choose a file to upload');
      return;
    }
    setState(() {
      _submitting = true;
      _error = null;
    });
    try {
      final file = await ref.read(contentRepositoryProvider).uploadFile(
            title: _titleController.text.trim(),
            description: _descriptionController.text.trim(),
            categoryId: _categoryId,
            tags: _tagsController.text.trim(),
            bytes: _picked!.bytes!,
            filename: _picked!.name,
          );
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Uploaded — pending admin moderation')));
        context.push('/files/${file.id}');
      }
    } catch (e) {
      setState(() => _error = e.toString());
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  @override
  void dispose() {
    _titleController.dispose();
    _descriptionController.dispose();
    _tagsController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Center(
      child: SingleChildScrollView(
        padding: const EdgeInsets.all(24),
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 560),
          child: Form(
            key: _formKey,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Text('Upload a file', style: theme.textTheme.headlineSmall),
                const SizedBox(height: 4),
                Text(
                  'Authorized content only — open-source software, research papers, '
                  'educational material, or university project archives. Every upload '
                  'is held for admin moderation before it becomes downloadable.',
                  style: theme.textTheme.bodySmall?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                ),
                const SizedBox(height: 24),
                OutlinedButton.icon(
                  onPressed: _pickFile,
                  icon: const Icon(Icons.attach_file),
                  label: Text(_picked == null ? 'Choose file' : _picked!.name),
                ),
                if (_picked != null)
                  Padding(
                    padding: const EdgeInsets.only(top: 4),
                    child: Text('${(_picked!.size / (1024 * 1024)).toStringAsFixed(2)} MB',
                        style: theme.textTheme.bodySmall),
                  ),
                const SizedBox(height: 16),
                TextFormField(
                  controller: _titleController,
                  decoration: const InputDecoration(labelText: 'Title'),
                  validator: (v) => (v == null || v.trim().isEmpty) ? 'Title is required' : null,
                ),
                const SizedBox(height: 16),
                TextFormField(
                  controller: _descriptionController,
                  decoration: const InputDecoration(labelText: 'Description'),
                  maxLines: 4,
                ),
                const SizedBox(height: 16),
                if (_categories.isNotEmpty)
                  DropdownButtonFormField<int?>(
                    value: _categoryId,
                    decoration: const InputDecoration(labelText: 'Category'),
                    items: [
                      const DropdownMenuItem(value: null, child: Text('Uncategorized')),
                      ..._categories.map((c) => DropdownMenuItem(value: c.id, child: Text(c.name))),
                    ],
                    onChanged: (v) => setState(() => _categoryId = v),
                  ),
                const SizedBox(height: 16),
                TextFormField(
                  controller: _tagsController,
                  decoration: const InputDecoration(
                      labelText: 'Tags (comma-separated)', hintText: 'os, linux, iso'),
                ),
                if (_error != null) ...[
                  const SizedBox(height: 16),
                  Text(_error!, style: TextStyle(color: theme.colorScheme.error)),
                ],
                const SizedBox(height: 24),
                FilledButton(
                  onPressed: _submitting ? null : _submit,
                  child: _submitting
                      ? const SizedBox(height: 20, width: 20, child: CircularProgressIndicator(strokeWidth: 2))
                      : const Text('Upload'),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
