from ultralytics import YOLO
 
# 加载模型
model = YOLO('yolov8n.pt')  # 加载官方模型（示例）
model = YOLO('F:/GroudControl/my_yolov8/runs/detect/train_zdp2/weights/best.pt')  # 加载自定义训练模型（示例）
 
# 导出模型
model.export(format='onnx')
""" import onnx

# 加载模型
model = onnx.load("F:/GroudControl/onnxruntime-inference-examples-main/onnxruntime-inference-examples-main/mobile/examples/object_detection/android/app/src/main/res/raw")
for output in model.graph.output:
    print("Output name:", output.name)
 """