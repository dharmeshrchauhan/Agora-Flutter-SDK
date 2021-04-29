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

class ARCameraView: UIView {
    private var arView: ARView!
    private var deepAr: DeepAR!
    public var deepARDelegate: DeepARDelegate!
    private var cameraController: CameraController!
    private var btn: UIButton!
    
    public func setupDeepAR() {
        self.deepAr = DeepAR()
        self.deepAr.setLicenseKey("4c92426f96aba754a1c22f8ee4624dd127c46772e75af4621284a4fad9592d5db1414470d5522a20")
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
        self.deepAr.switchEffect(withSlot: "masks", path:
                                                Bundle(identifier: "org.cocoapods.agora-rtc-engine")?.path(forResource: "flower_crown", ofType: nil))
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
