from flask import Flask, render_template, request, redirect, url_for, send_from_directory
from flask_sqlalchemy import SQLAlchemy
import numpy as np
from ultralytics import YOLO
import os
import cv2
from datetime import datetime

app = Flask(__name__)

# 配置数据库连接
app.config['SQLALCHEMY_DATABASE_URI'] = 'mysql+pymysql://root:root@localhost:3306/substationdb'
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False
db = SQLAlchemy(app)

# 定义日志模型
class DetectionLog(db.Model):
    __tablename__ = 'detection_logs'
    LogNo = db.Column(db.Integer, primary_key=True)
    FileName = db.Column(db.String(255), nullable=False)
    DetectionTime = db.Column(db.DateTime, default=datetime.now)
    UploadedFilePath = db.Column(db.String(255), nullable=False)
    DetectedFilePath = db.Column(db.String(255), nullable=False)

    def __init__(self, FileName, UploadedFilePath, DetectedFilePath):
        self.FileName = FileName
        self.UploadedFilePath = UploadedFilePath
        self.DetectedFilePath = DetectedFilePath

# 测试数据库连接是否成功
@app.route('/test_db')
def test_db():
    try:
        db.session.query('1').from_statement('SELECT 1').all()
        return 'Database connection successful!'
    except Exception as e:
        return f'Database connection failed: {e}'

# 设置上传文件和检测结果的目录
UPLOAD_FOLDER = 'web/static/uploads'
DETECTED_FOLDER = 'web/static/detected'
BASE_FOLDER = '/static'
os.makedirs(UPLOAD_FOLDER, exist_ok=True)
os.makedirs(DETECTED_FOLDER, exist_ok=True)

app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER
app.config['DETECTED_FOLDER'] = DETECTED_FOLDER

# 加载 YOLO 模型
#model = YOLO("F:/OneDrive/桌面/my_yolov8/my_yolov8/ultralytics/cfg/models/v8/yolov8m-CA.yaml")
model_path = "F:/OneDrive/桌面/my_yolov8/my_yolov8/runs/detect/train_815/weights/best.pt"
model = YOLO(model_path)

@app.route('/')
def index():
    return render_template('index.html')

@app.route('/detectImage', methods=['GET', 'POST'])
def detect_image():
    if request.method == 'POST':
        if 'image' not in request.files:
            return redirect(request.url)

        file = request.files['image']
        if file.filename == '':
            return redirect(request.url)

        file_path = os.path.join(app.config['UPLOAD_FOLDER'], file.filename)
        file.save(file_path)

        # 使用模型进行检测
        results = model(file_path)

        # 保存检测结果
        detected_file_path = os.path.join(app.config['DETECTED_FOLDER'], file.filename)
        for i, result in enumerate(results):
            result.save(detected_file_path)

        # 保存日志到数据库
        log = DetectionLog(FileName=file.filename, UploadedFilePath=file_path, DetectedFilePath=detected_file_path)
        db.session.add(log)
        db.session.commit()

        return render_template('detectImage.html', 
                               uploaded_image_url=BASE_FOLDER+url_for('uploaded_file', filename=file.filename),
                               detected_image_url=BASE_FOLDER+url_for('detected_file', filename=file.filename))
    return render_template('detectImage.html')


@app.route('/detectVideo', methods=['GET', 'POST'])
def detect_video():
    if request.method == 'POST':
        if 'video' not in request.files:
            return redirect(request.url)
        
        file = request.files['video']
        if file.filename == '':
            return redirect(request.url)
        
        file_path = os.path.join(app.config['UPLOAD_FOLDER'], file.filename)
        file.save(file_path)
        
        # 使用模型进行检测并保存结果视频
        detected_file_path = os.path.join(app.config['DETECTED_FOLDER'], file.filename)
        detect_and_save_video(file_path, detected_file_path)
        
        # 保存日志到数据库
        log = DetectionLog(FileName=file.filename, UploadedFilePath=file_path, DetectedFilePath=detected_file_path)
        db.session.add(log)
        db.session.commit()

        return render_template('detectVideo.html', 
                               uploaded_video_url=BASE_FOLDER+url_for('uploaded_file', filename=file.filename),
                               detected_video_url=BASE_FOLDER+url_for('detected_file', filename=file.filename))
    return render_template('detectVideo.html')


@app.route('/uploads/<filename>')
def uploaded_file(filename):
    return send_from_directory(app.config['UPLOAD_FOLDER'], filename)

@app.route('/detected/<filename>')
def detected_file(filename):
    return send_from_directory(app.config['DETECTED_FOLDER'], filename)

@app.route('/logs', methods=['GET', 'POST'])
def logs():
    query = DetectionLog.query
    dates = db.session.query(db.func.date(DetectionLog.DetectionTime)).distinct().all()
    dates = [date[0].strftime("%Y-%m-%d") for date in dates]

    if request.method == 'POST':
        search_by_name = request.form.get('search_by_name', '')
        search_by_date = request.form.get('search_by_date', '')

        if search_by_name:
            query = query.filter(DetectionLog.FileName.like(f'%{search_by_name}%'))
        if search_by_date:
            query = query.filter(db.func.date(DetectionLog.DetectionTime) == search_by_date)
    
    logs = query.all()
    image_logs = [log for log in logs if log.FileName.endswith(('.png', '.jpg', '.jpeg'))]
    video_logs = [log for log in logs if log.FileName.endswith(('.mp4', '.avi'))]

    return render_template('detectLogs.html', image_logs=image_logs, video_logs=video_logs, dates=dates)

@app.route('/log/<int:log_id>/<log_type>')
def show_log(log_id, log_type):
    log = DetectionLog.query.get_or_404(log_id)
    uploaded_url = BASE_FOLDER+url_for('uploaded_file', filename=os.path.basename(log.UploadedFilePath))
    detected_url = BASE_FOLDER+url_for('detected_file', filename=os.path.basename(log.DetectedFilePath))
    return render_template('LogShow.html', uploaded_url=uploaded_url, detected_url=detected_url, log_type=log_type)


def detect_and_save_video(input_video_path, output_video_path):
    cap = cv2.VideoCapture(input_video_path)
    frame_rate_divider = 1
    frame_count = 0
    
    # 获取视频信息
    fourcc = cv2.VideoWriter_fourcc(*'avc1')
    fps = cap.get(cv2.CAP_PROP_FPS)
    width = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
    height = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
    out = cv2.VideoWriter(output_video_path, fourcc, fps, (width, height))

    while cap.isOpened():
        ret, frame = cap.read()
        if not ret:
            break
        
        if frame_count % frame_rate_divider == 0:
            results = model(frame)
            for result in results:
                for box in result.boxes:
                    class_id = result.names[box.cls[0].item()]
                    x1, y1, x2, y2 = box.xyxy[0].int().tolist()
                    cv2.rectangle(frame, (x1, y1), (x2, y2), (255, 0, 0), 2)
                    cv2.putText(frame, class_id, (x1, y1 - 10), cv2.FONT_HERSHEY_SIMPLEX, 0.9, (255, 0, 0), 2)
        
        frame = frame.astype(np.uint8)
        out.write(frame)
        frame_count += 1

    cap.release()
    out.release()

if __name__ == '__main__':
    app.run(debug=True)
