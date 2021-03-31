import { WebPlugin } from '@capacitor/core';
import { PhotopickerPlugin, PhotopickerResponse } from './definitions';


export class PhotopickerWeb extends WebPlugin implements PhotopickerPlugin {
  constructor() {
    super({
      name: 'Photopicker',
      platforms: ['web'],
    });
  }

  async getPhotos(): Promise<PhotopickerResponse> {
    console.log('GETPHOTOS');
    return Promise.reject('not implemented');
  }
}

const Photopicker = new PhotopickerWeb();

export { Photopicker };

import { registerWebPlugin } from '@capacitor/core';
registerWebPlugin(Photopicker);
