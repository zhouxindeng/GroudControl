import gc

import torch
from ultralytics import YOLO

def release_gpu_memory():
    gc.collect()
    if torch.cuda.is_available():
        torch.cuda.empty_cache()

if __name__ == '__main__':
    release_gpu_memory()
    model = YOLO("yolov8n.pt")
    #model.train(data="F:/GroudControl/my_yolov8/datasets/NWPU VHR-10 dataset/VHR-10.yaml", epochs=30)
    model.train(data="F:/GroudControl/my_yolov8/datasets/NWPU VHR-10 dataset/VHR-10.yaml", epochs=30,batch=2)
    result = model.val()

