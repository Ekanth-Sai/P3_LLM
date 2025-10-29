import { Component, OnInit, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { RouterOutlet } from '@angular/router';
import { AuthService } from './services/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrls: ['./app.css']
})
export class App implements OnInit {
  protected readonly title = signal('frontend');

  private authService = inject(AuthService);
  private router = inject(Router);

  ngOnInit(): void {
    // Check token immediately on app load
    if (this.authService.isTokenExpired()) {
      this.authService.logout();
      this.router.navigate(['/login']);
    }


    //  auto-check every minute while app is open
    setInterval(() => {
      if (this.authService.isTokenExpired()) {
        this.authService.logout();
        this.router.navigate(['/login']);
      }
    }, 60 * 1000); // 1 minute

  }
}
