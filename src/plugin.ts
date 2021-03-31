import { Plugins } from '@capacitor/core';
import { PhotopickerResponse } from './definitions';

const { Photopicker } = Plugins;

export class EAGPhotopicker {
    constructor() { }
    getPhotos(): Promise<PhotopickerResponse> {
        return Photopicker.getPhotos();
    }
}