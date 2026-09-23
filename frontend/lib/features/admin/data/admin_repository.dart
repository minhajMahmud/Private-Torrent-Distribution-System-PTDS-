import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/dio_client.dart';
import '../../content/data/content_models.dart';

class AdminDashboardStats {
  final int totalUsers;
  final int totalFiles;
  final int pendingModeration;
  final int approvedFiles;
  final int rejectedFiles;
  final int totalCategories;
  final int downloadsLast7Days;
  final int downloadsLast30Days;
  final int totalDownloadsAllTime;
  final int lockedUsers;

  const AdminDashboardStats({
    required this.totalUsers,
    required this.totalFiles,
    required this.pendingModeration,
    required this.approvedFiles,
    required this.rejectedFiles,
    required this.totalCategories,
    required this.downloadsLast7Days,
    required this.downloadsLast30Days,
    required this.totalDownloadsAllTime,
    required this.lockedUsers,
  });

  factory AdminDashboardStats.fromJson(Map<String, dynamic> json) => AdminDashboardStats(
        totalUsers: json['totalUsers'] as int? ?? 0,
        totalFiles: json['totalFiles'] as int? ?? 0,
        pendingModeration: json['pendingModeration'] as int? ?? 0,
        approvedFiles: json['approvedFiles'] as int? ?? 0,
        rejectedFiles: json['rejectedFiles'] as int? ?? 0,
        totalCategories: json['totalCategories'] as int? ?? 0,
        downloadsLast7Days: json['downloadsLast7Days'] as int? ?? 0,
        downloadsLast30Days: json['downloadsLast30Days'] as int? ?? 0,
        totalDownloadsAllTime: json['totalDownloadsAllTime'] as int? ?? 0,
        lockedUsers: json['lockedUsers'] as int? ?? 0,
      );
}

class AdminUserItem {
  final String id;
  final String username;
  final String email;
  final bool enabled;
  final bool locked;
  final Set<String> roles;
  final int uploadCount;

  const AdminUserItem({
    required this.id,
    required this.username,
    required this.email,
    required this.enabled,
    required this.locked,
    required this.roles,
    required this.uploadCount,
  });

  factory AdminUserItem.fromJson(Map<String, dynamic> json) => AdminUserItem(
        id: json['id'] as String,
        username: json['username'] as String,
        email: json['email'] as String,
        enabled: json['enabled'] as bool? ?? true,
        locked: json['locked'] as bool? ?? false,
        roles: Set<String>.from(json['roles'] as List? ?? const []),
        uploadCount: json['uploadCount'] as int? ?? 0,
      );
}

class AdminRepository {
  final Dio _dio;
  AdminRepository(this._dio);

  Future<AdminDashboardStats> dashboard() async {
    final res = await _dio.get('/admin/dashboard');
    return AdminDashboardStats.fromJson(res.data);
  }

  Future<List<AdminUserItem>> listUsers({int page = 0, int size = 50}) async {
    final res = await _dio.get('/admin/users', queryParameters: {'page': page, 'size': size});
    return (res.data['content'] as List).map((e) => AdminUserItem.fromJson(e)).toList();
  }

  Future<void> updateUser(String userId, {bool? locked, bool? enabled, String? role}) async {
    await _dio.patch('/admin/users/$userId', data: {
      if (locked != null) 'locked': locked,
      if (enabled != null) 'enabled': enabled,
      if (role != null) 'role': role,
    });
  }

  Future<PageResult<FileItem>> moderationQueue({int page = 0, int size = 20}) async {
    final res = await _dio.get('/admin/moderation-queue', queryParameters: {'page': page, 'size': size});
    return PageResult.fromJson(res.data, (e) => FileItem.fromJson(e));
  }

  Future<void> moderate(String fileId, {required bool approve, String? rejectionReason}) async {
    await _dio.post('/admin/files/$fileId/moderate', data: {
      'decision': approve ? 'APPROVE' : 'REJECT',
      if (rejectionReason != null) 'rejectionReason': rejectionReason,
    });
  }

  Future<CategoryItem> createCategory(String name, {String? description}) async {
    final res = await _dio.post('/categories', data: {'name': name, if (description != null) 'description': description});
    return CategoryItem.fromJson(res.data);
  }

  Future<void> deleteCategory(int id) async {
    await _dio.delete('/categories/$id');
  }
}

final adminRepositoryProvider = Provider<AdminRepository>((ref) {
  return AdminRepository(ref.watch(dioProvider));
});
