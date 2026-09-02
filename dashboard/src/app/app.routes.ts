import {Routes} from '@angular/router';

/**
 * The task list is the entry point, not the matrix: a table leaves it to the
 * reader to work out what needs doing.
 */
export const routes: Routes = [
  {path: '', pathMatch: 'full', redirectTo: 'tasks'},
  {path: 'tasks', loadComponent: () => import('./pages/tasks/tasks.component').then((m) => m.TasksComponent)},
  {path: 'matrix', loadComponent: () => import('./pages/matrix/matrix.component').then((m) => m.MatrixComponent)},
  {
    path: 'components/:id',
    loadComponent: () => import('./pages/component-detail/component-detail.component')
      .then((m) => m.ComponentDetailComponent)
  },
  {
    path: 'master-data',
    loadComponent: () => import('./pages/master-data/master-data.component').then((m) => m.MasterDataComponent)
  },
  {path: '**', redirectTo: 'tasks'}
];
