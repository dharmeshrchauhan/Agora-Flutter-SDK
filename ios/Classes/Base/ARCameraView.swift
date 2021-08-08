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
    
    public func setupDeepAR(deepArLicenseKey: String) {
        self.deepAr = DeepAR()
        self.deepAr.setLicenseKey(deepArLicenseKey)
        self.deepAr.delegate = deepARDelegate
    }
    
    public func setupARCamera() {
        //let rect = CGRect(x: 0, y: 0, width: 720, height: 1280)
        let arviewFrame = self.bounds.width > 0 ? self.bounds : CGRect(x: 0, y: 0, width: 1, height: 1)
        self.arView = (self.deepAr.createARView(withFrame: arviewFrame) as! ARView)
        self.arView.contentMode = ContentMode.scaleAspectFill
        self.addSubview(self.arView)
        NSLayoutConstraint.activate([
                                        self.arView.leftAnchor.constraint(equalTo: self.leftAnchor, constant: 0),
                                        self.arView.rightAnchor.constraint(equalTo: self.rightAnchor, constant: 0),
                                        self.arView.topAnchor.constraint(equalTo: self.topAnchor, constant: 0),
                                        self.arView.bottomAnchor.constraint(equalTo: self.bottomAnchor, constant: 0)]);
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
    
    public func endDeepAR() {
        self.arView?.stopFrameOutput()
        self.deepAr.shutdown()
    }
    
    public func switchCamera() {
        if (self.cameraController.position == .front){
        self.cameraController.position = .back;
        }
        else {
            self.cameraController.position = .front;
        }
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
