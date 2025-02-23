from ultralytics import YOLO
 
if __name__=="__main__":
    
    pth_path="F:/GroudControl/my_yolov8/runs/detect/train_zdp2/weights/best.pt"
 
    test_path="F:/GroudControl/my_yolov8/datasets/NWPU VHR-10 dataset/train/images/807.jpg"
    # Load a model
    #model = YOLO('yolov8n.pt')  # load an official model
    model = YOLO(pth_path)  # load a custom model
 
    # Predict with the model
    results = model(test_path,save=True,conf=0.5)  # predict on an image
