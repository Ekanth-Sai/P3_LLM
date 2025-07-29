import { Routes } from '@angular/router';
import { LandingComponent } from './landing/landing';
import { LoginComponent } from './login/login';
import { SignupComponent } from './signup/signup';
import { BotUsageComponent } from './bot-usage/bot-usage';
import { SignupConfirmationComponent } from './signup-confirmation/signup-confirmation';
import { AdminComponent } from './admin/admin';

export const routes: Routes = [
    { path: '', component: LandingComponent },
    { path: 'login', component: LoginComponent },
    { path: 'signup', component: SignupComponent },
    { path: 'bot-usage', component: BotUsageComponent },
    { path: 'admin', component: AdminComponent },
    // { path: '', redirectTo: 'login', pathMatch: 'full' },
    { path: 'signup-confirmation', component: SignupConfirmationComponent },
    { path: '**', redirectTo: '', pathMatch: 'full' }
];
