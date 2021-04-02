//
//  PluginTypes.swift
//  Plugin
//
//  Copyright © 2021 Philipp Anné
//

import Foundation


internal enum PhotopickerPropertyListKeys: String, CaseIterable {

    case photoLibraryUsage = "NSPhotoLibraryUsageDescription"
    
    var link: String {
        switch self {
        case .photoLibraryUsage:
            return "https://developer.apple.com/library/content/documentation/General/Reference/InfoPlistKeyReference/Articles/CocoaKeys.html#//apple_ref/doc/uid/TP40009251-SW17"
        }
    }

    var missingMessage: String {
        return "You are missing \(self.rawValue) in your Info.plist file." +
            " Camera will not function without it. Learn more: \(self.link)"
    }
}
