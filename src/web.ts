import { WebPlugin } from '@capacitor/core';
import { PhotopickerPlugin, PhotopickerResponse, PhotopickerOptions } from './definitions';


export class PhotopickerWeb extends WebPlugin implements PhotopickerPlugin {
  constructor() {
    super({
      name: 'Photopicker',
      platforms: ['web'],
    });
  }

  async getPhotos(options: PhotopickerOptions): Promise<PhotopickerResponse> {
    console.log('GETPHOTOS');
    return Promise.reject('not implemented');
  }
}

const Photopicker = new PhotopickerWeb();

export { Photopicker };

import { registerPlugin } from '@capacitor/core';
registerPlugin(Photopicker);
