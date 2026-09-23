import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../../../core/network/dio_client.dart';
import '../../auth/application/auth_controller.dart';

class ProfileScreen extends ConsumerStatefulWidget {
  const ProfileScreen({super.key});

  @override
  ConsumerState<ProfileScreen> createState() => _ProfileScreenState();
}

class _ProfileScreenState extends ConsumerState<ProfileScreen> {
  Map<String, dynamic>? _profile;
  List<dynamic> _history = [];
  bool _loading = true;
  final _currentPasswordController = TextEditingController();
  final _newPasswordController = TextEditingController();
  bool _changingPassword = false;
  String? _passwordMessage;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    final dio = ref.read(dioProvider);
    try {
      final profileRes = await dio.get('/users/me');
      final historyRes = await dio.get('/downloads/history', queryParameters: {'size': 10});
      setState(() {
        _profile = profileRes.data as Map<String, dynamic>;
        _history = (historyRes.data['content'] as List?) ?? [];
      });
    } on DioException catch (_) {
      // Leave the screen usable even if history fails to load.
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _changePassword() async {
    setState(() {
      _changingPassword = true;
      _passwordMessage = null;
    });
    try {
      await ref.read(dioProvider).post('/users/me/change-password', data: {
        'currentPassword': _currentPasswordController.text,
        'newPassword': _newPasswordController.text,
      });
      setState(() => _passwordMessage = 'Password updated successfully');
      _currentPasswordController.clear();
      _newPasswordController.clear();
    } on DioException catch (e) {
      final data = e.response?.data;
      setState(() => _passwordMessage =
          data is Map && data['message'] != null ? data['message'].toString() : 'Failed to change password');
    } finally {
      if (mounted) setState(() => _changingPassword = false);
    }
  }

  @override
  void dispose() {
    _currentPasswordController.dispose();
    _newPasswordController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    if (_loading) return const Center(child: CircularProgressIndicator());
    final authState = ref.watch(authControllerProvider);
    final username = authState is AuthAuthenticated ? authState.user.username : '';

    return Center(
      child: SingleChildScrollView(
        padding: const EdgeInsets.all(24),
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 600),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Center(
                child: CircleAvatar(
                  radius: 36,
                  child: Text(username.isNotEmpty ? username[0].toUpperCase() : '?',
                      style: theme.textTheme.headlineMedium),
                ),
              ),
              const SizedBox(height: 12),
              Center(child: Text(_profile?['username'] ?? username, style: theme.textTheme.titleLarge)),
              Center(child: Text(_profile?['email'] ?? '', style: theme.textTheme.bodyMedium)),
              const SizedBox(height: 24),
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      Text('Change password', style: theme.textTheme.titleMedium),
                      const SizedBox(height: 12),
                      TextField(
                        controller: _currentPasswordController,
                        decoration: const InputDecoration(labelText: 'Current password'),
                        obscureText: true,
                      ),
                      const SizedBox(height: 12),
                      TextField(
                        controller: _newPasswordController,
                        decoration: const InputDecoration(labelText: 'New password'),
                        obscureText: true,
                      ),
                      if (_passwordMessage != null) ...[
                        const SizedBox(height: 8),
                        Text(_passwordMessage!, style: theme.textTheme.bodySmall),
                      ],
                      const SizedBox(height: 16),
                      FilledButton(
                        onPressed: _changingPassword ? null : _changePassword,
                        child: const Text('Update password'),
                      ),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 24),
              Text('Recent downloads', style: theme.textTheme.titleMedium),
              const SizedBox(height: 8),
              if (_history.isEmpty)
                const Text('No downloads yet')
              else
                ..._history.map((h) => ListTile(
                      contentPadding: EdgeInsets.zero,
                      leading: const Icon(Icons.download_done_outlined),
                      title: Text(h['fileTitle'] ?? ''),
                      trailing: h['downloadedAt'] != null
                          ? Text(DateFormat.yMMMd().format(DateTime.parse(h['downloadedAt'])))
                          : null,
                    )),
            ],
          ),
        ),
      ),
    );
  }
}
