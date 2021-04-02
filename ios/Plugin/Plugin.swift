import Foundation
import Capacitor

import os
import PhotosUI


// https://developer.apple.com/forums/thread/652496
// https://christianselig.com/2020/09/phpickerviewcontroller-efficiently/

/**
 * Please read the Capacitor iOS Plugin Development Guide
 * here: https://capacitorjs.com/docs/plugins/ios
 */
@available(iOS 14, *)
@objc(Photopicker)
public class Photopicker: CAPPlugin {
    
    private var call: CAPPluginCall?
    
    // ⚡ ⛔ ✅ 📎 🐭 👎
    
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
        
           // present
/*           picker.modalPresentationStyle = settings.presentationStyle
           if settings.presentationStyle == .popover {
               picker.popoverPresentationController?.delegate = self
               setCenteredPopover(picker)
           }
  */
        self.bridge.viewController.present(picker, animated: true)
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
                
                guard let documentsDirectory = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first else {
                    
                    CAPLog.print("⚡️ ", self.pluginId!, "-", "doc dir failed")
                    dispatchQueue.sync { totalConversionsCompleted += 1 }
                    
                    return
                }

                let fileURL = documentsDirectory.appendingPathComponent("py_temp_" + UUID().uuidString)
                
                let sourceOptions = [kCGImageSourceShouldCache: false] as CFDictionary
                
                guard let source = CGImageSourceCreateWithURL(url as CFURL, sourceOptions) else {
                    dispatchQueue.sync { totalConversionsCompleted += 1 }
                    
                    CAPLog.print("⚡️ ", self.pluginId!, "-", "source guard failed")
                    
                    return
                }
                
                let downsampleOptions = [
                    kCGImageSourceCreateThumbnailFromImageAlways: true,
                    kCGImageSourceCreateThumbnailWithTransform: true,
                    kCGImageSourceThumbnailMaxPixelSize: 500, //2_000,
                ] as CFDictionary

                guard let cgImage = CGImageSourceCreateThumbnailAtIndex(source, 0, downsampleOptions) else {
                    
                    CAPLog.print("⚡️ ", self.pluginId!, "-", "cgimage guard failed")
                    dispatchQueue.sync { totalConversionsCompleted += 1 }
                    return
                }

               // let data = NSMutableData()
                
                guard let imageDestination = CGImageDestinationCreateWithURL(fileURL as CFURL, UTType.jpeg.identifier as CFString, 1, nil) else {
                    
                    
                    CAPLog.print("⚡️ ", self.pluginId!, "-", "imgDestination guard failed")
                    dispatchQueue.sync { totalConversionsCompleted += 1 }
                    
                    return
                }
                
                /*guard let imageDestination = CGImageDestinationCreateWithData(data, UTType.jpeg.identifier as CFString, 1, nil) else {
                    dispatchQueue.sync { totalConversionsCompleted += 1 }
                    return
                }*/
                
                // Don't compress PNGs, they're too pretty
                let isPNG: Bool = {
                    guard let utType = cgImage.utType else { return false }
                    return (utType as String) == UTType.png.identifier
                }()

                let destinationProperties = [
                    kCGImageDestinationLossyCompressionQuality: isPNG ? 1.0 : 0.50
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
                        
                        let urlArray = selectedImageDatas.filter {
                            $0 != nil
                        }.map {
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
