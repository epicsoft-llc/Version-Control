import {Component} from '@angular/core';
import {TranslatePipe} from '@ngx-translate/core';

@Component({
  selector: 'vc-master-data',
  imports: [TranslatePipe],
  templateUrl: './master-data.component.html'
})
export class MasterDataComponent {
}
