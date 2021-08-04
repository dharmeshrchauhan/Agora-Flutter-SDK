//
//  RtcSurfaceView.swift
//  RCTAgora
//
//  Created by LXH on 2020/4/15.
//  Copyright © 2020 Syan. All rights reserved.
//

import UIKit
import DeepAR
import AgoraRtcKit

extension String {
    var path: String? {
        return Bundle(identifier: "org.cocoapods.agora-rtc-engine")?.path(forResource: self, ofType: nil)
    }
}

enum Masks: String, CaseIterable {
    case none
    case beauty_without_deform
    case beauty_without_eyelashes
    case beard
    case ball_face
    case background_segmentation
    case alien
    case aviators
    case fairy_lights
    case flower_crown
    case frankenstein
    case hair_segmentation
    case lion
    case manly_face
    case plastic_ocean
    case pumpkin
    case scuba
    case tape_face
    case tiny_sunglasses
    case topology
}

class ARCameraView: UIView {
    private var arView: ARView!
    private var deepAr: DeepAR!
    private var maskPaths: [String?] {
        return Masks.allCases.map { $0.rawValue.path }
    }
    private var currentIndex = 0
    private func nextMaskPath() -> String? {
        currentIndex = (currentIndex + 1) % maskPaths.count
        return maskPaths[currentIndex]
    }
        
    public var deepARDelegate: DeepARDelegate!
    private var cameraController: CameraController!
    private var btn: UIButton!
    
    public func setupDeepAR() {
        self.deepAr = DeepAR()
        self.deepAr.setLicenseKey("d1664350dbc926e3f73c66716f244eda0d65df1c7d58f9b870c30405ae83dea8da5e21ce2d5f3b62")
        self.deepAr.delegate = deepARDelegate
    }
    
    public func setupARCamera() {
        //let rect = CGRect(x: 0, y: 0, width: 720, height: 1280)
        let arviewFrame = self.bounds.width > 0 ? self.bounds : CGRect(x: 0, y: 0, width: 300, height: 300)
        self.arView = (self.deepAr.createARView(withFrame: arviewFrame) as! ARView)
        self.addSubview(self.arView)
//        self.btn = UIButton(frame: CGRect(x: 20, y: 30, width: 150, height: 30));
//        self.btn.setTitle("Set Beauty Filter", for: .normal)
//        self.btn.addTarget(self, action: #selector(toggleBeautyFilter(_:)), for: .touchUpInside)
//        self.addSubview(self.btn)
        self.cameraController = CameraController()
        self.cameraController.deepAR = self.deepAr
        self.cameraController.startCamera()

        DispatchQueue.main.asyncAfter(deadline: .now() + .seconds(1)) {
            self.deepAr.startCapture(withOutputWidth: 720, outputHeight: 1280, subframe: CGRect(x: 0.0, y: 0.0, width: 1.0, height: 1.0))
        }
    }
    
    public func setNextARFilter() {
        self.deepAr.switchEffect(withSlot: "masks", path: self.nextMaskPath())
    }
    
    override var bounds: CGRect {
        didSet {
            if self.arView != nil {
                self.arView.frame = self.bounds;
            }
        }
    }
    
    override var frame: CGRect {
        didSet {
            if self.arView != nil {
                self.arView.frame = self.bounds;
            }
        }
    }
}
