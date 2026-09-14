export * from './auth.service';
import { AuthApi } from './auth.service';
export * from './me.service';
import { MeApi } from './me.service';
export * from './practice.service';
import { PracticeApi } from './practice.service';
export * from './users.service';
import { UsersApi } from './users.service';
export const APIS = [AuthApi, MeApi, PracticeApi, UsersApi];
