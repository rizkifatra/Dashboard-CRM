import { Routes } from '@angular/router';
import { FiscalDashboardComponent } from './dashboard/fiscal-dashboard.component';
import { StaffComponent } from './staff/staff.component';
import { ActivitiesComponent } from './activities/activities.component';
import { AccountsComponent } from './accounts/accounts.component';
import { OpportunitiesComponent } from './opportunities/opportunities.component';
import { LoginComponent } from './auth/login.component';
import { AuthCallbackComponent } from './auth/auth-callback.component';
import { authGuard } from './guards/auth.guard';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'auth/callback', component: AuthCallbackComponent },
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
  {
    path: 'dashboard',
    component: FiscalDashboardComponent,
    canActivate: [authGuard],
  },
  { path: 'staff', component: StaffComponent, canActivate: [authGuard] },
  {
    path: 'activities',
    component: ActivitiesComponent,
    canActivate: [authGuard],
  },
  { path: 'accounts', component: AccountsComponent, canActivate: [authGuard] },
  {
    path: 'opportunities',
    component: OpportunitiesComponent,
    canActivate: [authGuard],
  },
];
