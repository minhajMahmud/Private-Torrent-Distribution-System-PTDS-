import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/dio_client.dart';
import '../data/auth_models.dart';
import '../data/auth_repository.dart';

sealed class AuthState {
  const AuthState();
}

class AuthInitial extends AuthState {
  const AuthInitial();
}

class AuthLoading extends AuthState {
  const AuthLoading();
}

class AuthAuthenticated extends AuthState {
  final AppUser user;
  const AuthAuthenticated(this.user);
}

class AuthUnauthenticated extends AuthState {
  const AuthUnauthenticated();
}

class AuthError extends AuthState {
  final String message;
  const AuthError(this.message);
}

class AuthController extends StateNotifier<AuthState> {
  final AuthRepository _repository;
  final TokenStorage _tokenStorage;

  AuthController(this._repository, this._tokenStorage) : super(const AuthInitial());

  Future<void> login(String usernameOrEmail, String password) async {
    state = const AuthLoading();
    try {
      final result = await _repository.login(
        usernameOrEmail: usernameOrEmail,
        password: password,
      );
      await _tokenStorage.saveTokens(
        access: result.accessToken,
        refresh: result.refreshToken,
      );
      state = AuthAuthenticated(result.user);
    } on AuthApiException catch (e) {
      state = AuthError(e.message);
    }
  }

  Future<bool> register({
    required String username,
    required String email,
    required String password,
    String? fullName,
  }) async {
    state = const AuthLoading();
    try {
      await _repository.register(
        username: username,
        email: email,
        password: password,
        fullName: fullName,
      );
      state = const AuthUnauthenticated();
      return true;
    } on AuthApiException catch (e) {
      state = AuthError(e.message);
      return false;
    }
  }

  Future<void> logout() async {
    await _tokenStorage.clear();
    state = const AuthUnauthenticated();
  }
}

final authControllerProvider =
    StateNotifierProvider<AuthController, AuthState>((ref) {
  return AuthController(
    ref.watch(authRepositoryProvider),
    ref.watch(tokenStorageProvider),
  );
});
