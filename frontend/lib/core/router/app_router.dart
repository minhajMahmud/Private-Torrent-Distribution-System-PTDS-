import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../features/auth/application/auth_controller.dart';
import '../../features/auth/presentation/forgot_password_screen.dart';
import '../../features/auth/presentation/login_screen.dart';
import '../../features/auth/presentation/register_screen.dart';
import '../../features/content/presentation/file_detail_screen.dart';
import '../../features/dashboard/presentation/dashboard_shell.dart';

final routerProvider = Provider<GoRouter>((ref) {
  return GoRouter(
    initialLocation: '/login',
    redirect: (context, state) {
      final authState = ref.read(authControllerProvider);
      final isAuthed = authState is AuthAuthenticated;
      final goingToAuthPage = ['/login', '/register', '/forgot-password']
          .contains(state.matchedLocation);

      if (!isAuthed && !goingToAuthPage) return '/login';
      if (isAuthed && goingToAuthPage) return '/dashboard';
      return null;
    },
    routes: [
      GoRoute(path: '/login', builder: (context, state) => const LoginScreen()),
      GoRoute(path: '/register', builder: (context, state) => const RegisterScreen()),
      GoRoute(
        path: '/forgot-password',
        builder: (context, state) => const ForgotPasswordScreen(),
      ),
      GoRoute(
        path: '/dashboard',
        builder: (context, state) => const DashboardShell(),
      ),
      GoRoute(
        path: '/files/:id',
        builder: (context, state) => FileDetailScreen(fileId: state.pathParameters['id']!),
      ),
    ],
  );
});
