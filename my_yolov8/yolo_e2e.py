# Copyright (c) Microsoft Corporation.
# Licensed under the MIT License.
import numpy

from pathlib import Path
import onnxruntime_extensions


def get_yolov8_model(onnx_model_name: str):
    # install yolov8
    from pip._internal import main as pipmain
    try:
        import ultralytics
    except ImportError:
        pipmain(['install', 'ultralytics'])
        import ultralytics
    pt_model = Path("yolov8n.pt")
    model = ultralytics.YOLO(str(pt_model))  # load a pretrained model
    success = model.export(format="onnx")  # export the model to ONNX format
    assert success, "Failed to export yolov8n.pt to onnx"
    import shutil
    shutil.move(pt_model.with_suffix('.onnx'), onnx_model_name)


def add_pre_post_processing_to_yolo(input_model_file: Path, output_model_file: Path):
    """Construct the pipeline for an end2end model with pre and post processing. 
    The final model can take raw image binary as inputs and output the result in raw image file.

    Args:
        input_model_file (Path): The onnx yolo model.
        output_model_file (Path): where to save the final onnx model.
    """
    if not Path(input_model_file).is_file():
        get_yolov8_model(input_model_file)

    from onnxruntime_extensions.tools import add_pre_post_processing_to_model as add_ppp
    #修改class数量
    add_ppp.yolo_detection(input_model_file, output_model_file, "jpg", onnx_opset=18)

def test_inference(onnx_model_file:Path):
    import onnxruntime as ort
    import numpy as np

    providers = ['CPUExecutionProvider']
    session_options = ort.SessionOptions()
    session_options.register_custom_ops_library(onnxruntime_extensions.get_library_path())

    image = np.frombuffer(open('F:/GroudControl/my_yolov8/datasets/NWPU VHR-10 dataset/test/images/009.jpg', 'rb').read(), dtype=np.uint8)
    session = ort.InferenceSession(str(onnx_model_file), providers=providers, sess_options=session_options)

    inname = [i.name for i in session.get_inputs()]
    inp = {inname[0]: image}
    outputs = session.run(['image_out'], inp)[0]
    open('F:/GroudControl/my_yolov8/datasets/NWPU VHR-10 dataset/result.jpg', 'wb').write(outputs)



if __name__ == '__main__':
    print("checking the model...")
    onnx_model_name = Path("F:/GroudControl/onnxruntime-inference-examples-main/onnxruntime-inference-examples-main/mobile/examples/object_detection/android/app/src/main/res/raw/best2.onnx")
    #onnx_e2e_model_name=Path("F:/GroudControl/onnxruntime-inference-examples-main/onnxruntime-inference-examples-main/mobile/examples/object_detection/android/app/src/main/res/raw/best_with_pre_post_processing.onnx")
    onnx_e2e_model_name = onnx_model_name.with_suffix(suffix=".with_pre_post_processing.onnx")
    add_pre_post_processing_to_yolo(onnx_model_name, onnx_e2e_model_name)
    test_inference(onnx_e2e_model_name)