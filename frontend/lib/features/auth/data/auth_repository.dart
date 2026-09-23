import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/dio_client.dart';
import 'auth_models.dart';

class AuthApiException implements Exception {
  final String message;
  AuthApiException(this.message);
  @override
  String toString() => message;
}

class AuthRepository {
  final Dio _dio;
  AuthRepository(this._dio);

  Future<void> register({
    required String username,
    required String email,
    required String password,
    String? fullName,
  }) async {
    try {
      await _dio.post('/auth/register', data: {
        'username': username,
        'email': email,
        'password': password,
        if (fullName != null && fullName.isNotEmpty) 'fullName': fullName,
      });
    } on DioException catch (e) {
      throw AuthApiException(_extractMessage(e));
    }
  }

  Future<AuthResult> login({
    required String usernameOrEmail,
    required String password,
  }) async {
    try {
      final response = await _dio.post('/auth/login', data: {
        'usernameOrEmail': usernameOrEmail,
        'password': password,
      });
      return AuthResult.fromJson(response.data as Map<String, dynamic>);
    } on DioException catch (e) {
      throw AuthApiException(_extractMessage(e));
    }
  }

  Future<void> forgotPassword(String email) async {
    try {
      await _dio.post('/auth/forgot-password', data: {'email': email});
    } on DioException catch (e) {
      throw AuthApiException(_extractMessage(e));
    }
  }

  Future<void> resetPassword(String token, String newPassword) async {
    try {
      await _dio.post('/auth/reset-password',
          data: {'token': token, 'newPassword': newPassword});
    } on DioException catch (e) {
      throw AuthApiException(_extractMessage(e));
    }
  }

  Future<void> verifyEmail(String token) async {
    try {
      await _dio.get('/auth/verify-email', queryParameters: {'token': token});
    } on DioException catch (e) {
      throw AuthApiException(_extractMessage(e));
    }
  }

  String _extractMessage(DioException e) {
    final data = e.response?.data;
    if (data is Map && data['message'] != null) {
      return data['message'].toString();
    }
    return e.message ?? 'Something went wrong. Please try again.';
  }
}

final authRepositoryProvider = Provider<AuthRepository>((ref) {
  return AuthRepository(ref.watch(dioProvider));
});
