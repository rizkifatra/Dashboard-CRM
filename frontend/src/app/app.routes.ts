import { Routes } from '@angular/router';
import { DashboardComponent } from './dashboard/dashboard.component';
import { StaffComponent } from './staff/staff.component';
import { ActivitiesComponent } from './activities/activities.component';
import { AccountsComponent } from './accounts/accounts.component';
import { OpportunitiesComponent } from './opportunities/opportunities.component';

export const routes: Routes = [
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
  { path: 'dashboard', component: DashboardComponent },
  { path: 'staff', component: StaffComponent },
  { path: 'activities', component: ActivitiesComponent },
  { path: 'accounts', component: AccountsComponent },
  { path: 'opportunities', component: OpportunitiesComponent },
];
