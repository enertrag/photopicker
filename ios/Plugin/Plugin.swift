// Copyright © 2021 Philipp Anné
//
// Permission is hereby granted, free of charge, to any person obtaining a copy of this
// software and associated documentation files (the “Software”), to deal in the Software
// without restriction, including without limitation the rights to use, copy, modify, merge,
// publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons
// to whom the Software is furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be
// included in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
// INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
// PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
// FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
// OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
// OTHER DEALINGS IN THE SOFTWARE.

import Foundation
import Capacitor

import os
import PhotosUI

// @see
// https://developer.apple.com/forums/thread/652496
// https://christianselig.com/2020/09/phpickerviewcontroller-efficiently/

@available(iOS 14, *)
@objc(Photopicker)
public class Photopicker: CAPPlugin {
    
    private var call: CAPPluginCall?
    private var options = PhotopickerOptions()
    
    /**
     * Checks whether the Info.plist file contains the necessary entries for the app permissions.
     */
    private func checkUsageDescriptions() -> String? {
        if let dict = Bundle.main.infoDictionary {
            for key in PhotopickerPropertyListKeys.allCases where dict[key.rawValue] == nil {
                return key.missingMessage
            }
        }
        return nil
    }
    
    @objc func getPhotos(_ call: CAPPluginCall) {
        
        CAPLog.print("🐭 ", self.pluginId!, "-", "entering getPhotos()")
        
        self.call = call
        self.options = photopickerOptions(from: call)
        
        if let invalidOptions = checkPhotopickerOptions() {
            
            CAPLog.print("⚡️ ", self.pluginId!, "-", invalidOptions)
            call.reject(invalidOptions)
            return
        }

        if let missingUsageDescription = checkUsageDescriptions() {
            CAPLog.print("⚡️ ", self.pluginId!, "-", missingUsageDescription)
            call.reject(missingUsageDescription)
            bridge?.alert("Photopicker Error", "Missing required usage description. See console for more information")
            return
        }
        
        DispatchQueue.main.async {
            self._getPhotos(call)
        }
    }
    
    private func photopickerOptions(from call: CAPPluginCall) -> PhotopickerOptions {
        
        CAPLog.print("🐭 ", self.pluginId!, "-", "entering photopickerOptions()")
        
        var result = PhotopickerOptions()
        
        result.maxSize = call.getInt("maxSize") ?? 0
        result.quality = Double(call.getInt("quality") ?? 90) / 100.0
        
        return result
    }
    
    private func checkPhotopickerOptions() -> String? {
        
        CAPLog.print("🐭 ", self.pluginId!, "-", "entering checkPhotopickerOptions()")

        if options.maxSize < 0 || options.maxSize > 10_000 {
            return "invalid value for parameter maxSize (0-10000)"
        }
        
        if options.quality < 0.1 || options.quality > 1.0 {
            return "invalid value for parameter quality (10-100)"
        }
        
        return nil
    }
    
    func _getPhotos(_ call: CAPPluginCall) {
        
        CAPLog.print("🐭 ", self.pluginId!, "-", "entering _getPhotos()")
        
        let authStatus = PHPhotoLibrary.authorizationStatus()
        if authStatus == .restricted || authStatus == .denied {
            CAPLog.print("⛔ ", self.pluginId!, "-", "user denied permission")
            self.call?.reject("User denied permission")
            return
        }
        
        let id = self.pluginId!
        
        if authStatus == .authorized {
            
            self.openPicker()
            
        } else {
            
            PHPhotoLibrary.requestAuthorization({ [weak self] (status) in
                if status == PHAuthorizationStatus.authorized {
                    DispatchQueue.main.async { [weak self] in
                        self?.openPicker()
                    }
                } else {
                    CAPLog.print("⛔ ", id, "-", "user denied permission")
                    self?.call?.reject("User denied permission")
                }
            })
        }
    }
    
    func openPicker() {
        
        CAPLog.print("🐭 ", self.pluginId!, "-", "entering openPicker()")
        
        var config = PHPickerConfiguration()
        config.selectionLimit = 0
        config.filter = .images
        
        let picker = PHPickerViewController(configuration: config)
        picker.delegate = self
        
        self.bridge?.viewController.present(picker, animated: true)
    }
}

@available(iOS 14, *)
extension Photopicker: PHPickerViewControllerDelegate {
    public func picker(_ picker: PHPickerViewController, didFinishPicking results: [PHPickerResult]) {
        
        CAPLog.print("🐭 ", self.pluginId!, "-", "entering picker 19 (_,didFinishPicking)")
        
        picker.dismiss(animated: true, completion: nil)
        guard let _ = results.first else {
            
            CAPLog.print("👎 ", self.pluginId!, "-", "user cancelled selection")
            self.call?.resolve([
                "selected": false,
                "urls": []
            ])
            return
        }
        
        let dispatchQueue = DispatchQueue(label: "com.enertrag.plugins.photopicker.convertqueue")
        var selectedImageDatas = [URL?](repeating: nil, count: results.count) // Awkwardly named, sure
        var totalConversionsCompleted = 0

        for (index, result) in results.enumerated() {
            
            CAPLog.print("🐭 ", self.pluginId!, "-", "entering enumeration")

                        
            result.itemProvider.loadFileRepresentation(forTypeIdentifier: UTType.image.identifier) { (url, error) in
                
                CAPLog.print("🐭 ", self.pluginId!, "-", "entering loadFileRepresentation")
                
                guard let url = url else {
                    CAPLog.print("⚡️ ", self.pluginId!, "-", "url guard failed")

                    dispatchQueue.sync { totalConversionsCompleted += 1 }
                    return
                }
                
                guard let cachesDirectory = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask).first else {
                    
                    CAPLog.print("⚡️ ", self.pluginId!, "-", "doc dir failed")
                    dispatchQueue.sync { totalConversionsCompleted += 1 }
                    
                    return
                }

                let fileURL = cachesDirectory.appendingPathComponent("py_temp_" + UUID().uuidString + ".jpeg")
                
                let sourceOptions = [kCGImageSourceShouldCache: false] as CFDictionary
                
                guard let source = CGImageSourceCreateWithURL(url as CFURL, sourceOptions) else {
                    dispatchQueue.sync { totalConversionsCompleted += 1 }
                    
                    CAPLog.print("⚡️ ", self.pluginId!, "-", "source guard failed")
                    
                    return
                }
                
                let downsampleOptions = [
                    kCGImageSourceCreateThumbnailFromImageAlways: true,
                    kCGImageSourceCreateThumbnailWithTransform: true,
                    kCGImageSourceThumbnailMaxPixelSize: self.options.maxSize
                ] as CFDictionary

                guard let cgImage = CGImageSourceCreateThumbnailAtIndex(source, 0, downsampleOptions) else {
                    
                    CAPLog.print("⚡️ ", self.pluginId!, "-", "cgimage guard failed")
                    dispatchQueue.sync { totalConversionsCompleted += 1 }
                    return
                }
                
                guard let imageDestination = CGImageDestinationCreateWithURL(fileURL as CFURL, UTType.jpeg.identifier as CFString, 1, nil) else {
                    
                    
                    CAPLog.print("⚡️ ", self.pluginId!, "-", "imgDestination guard failed")
                    dispatchQueue.sync { totalConversionsCompleted += 1 }
                    
                    return
                }

                let destinationProperties = [
                    kCGImageDestinationLossyCompressionQuality: self.options.quality
                ] as CFDictionary

                CGImageDestinationAddImage(imageDestination, cgImage, destinationProperties)
                CGImageDestinationFinalize(imageDestination)
                
                dispatchQueue.sync {
                    selectedImageDatas[index] = fileURL
                    totalConversionsCompleted += 1
                }
                
                CAPLog.print("📎 ", self.pluginId!, "-", "repeat")
                
                dispatchQueue.sync {
                    
                    if(totalConversionsCompleted >= results.count) {
                        
                        CAPLog.print("✅ ", self.pluginId!, "-", "done")
                        
                        let urlArray = selectedImageDatas.map {
                            $0?.absoluteString
                        }
                        
                        self.call?.resolve([
                            "selected": true,
                            "urls": urlArray
                        ])
                    }
                }
            }
        }
        
        CAPLog.print("✅ ", self.pluginId!, "-", "running")
        
        return
    }
}
