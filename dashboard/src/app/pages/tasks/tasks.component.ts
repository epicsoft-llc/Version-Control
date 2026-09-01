import {Component} from '@angular/core';
import {TranslatePipe} from '@ngx-translate/core';

@Component({
  selector: 'vc-tasks',
  imports: [TranslatePipe],
  templateUrl: './tasks.component.html'
})
export class TasksComponent {
}
