import xml.etree.ElementTree as ET
import os
import cv2
import numpy as np

classes = ['bjdsyc','bj_wkps','yw_nc','xmbhyc','kgg_ybh','gbps','yw_gkxfw','hxq_gjbs','bj_bpmh','jyz_pl','bj_bpps','sly_dmyw','wcaqm','wcgz','hxq_gjtps','xy']

def convert(size, box):
    dw = 1. / size[0]
    dh = 1. / size[1]
    x = (box[0] + box[1]) / 2.0
    y = (box[2] + box[3]) / 2.0
    w = box[1] - box[0]
    h = box[3] - box[2]
    x = x * dw
    w = w * dw
    y = y * dh
    h = h * dh
    return (x, y, w, h)

def convert_annotation(xmlpath, xmlname):
    try:
        tree = ET.parse(xmlpath)
        root = tree.getroot()
        
        img_file = os.path.join(imgpath, f'{xmlname[:-4]}.{postfix}')
        img = cv2.imdecode(np.fromfile(img_file, np.uint8), cv2.IMREAD_COLOR)
        
        if img is None:
            print(f'Error: Image {img_file} could not be loaded.')
            return
        
        h, w = img.shape[:2]
        res = []
        
        for obj in root.iter('object'):
            cls = obj.find('name').text
            if cls not in classes:
                print(f'Error: class {cls} could not be define.')
                return
                #classes.append(cls)
            cls_id = classes.index(cls)
            xmlbox = obj.find('bndbox')
            b = (float(xmlbox.find('xmin').text), float(xmlbox.find('xmax').text), 
                 float(xmlbox.find('ymin').text), float(xmlbox.find('ymax').text))
            bb = convert((w, h), b)
            res.append(f'{cls_id} ' + ' '.join([f'{a:.6f}' for a in bb]))
        
        if res:
            txtname = xmlname[:-4] + '.txt'
            txtfile = os.path.join(txtpath, txtname)
            with open(txtfile, 'w') as f:
                f.write('\n'.join(res))
            print(f'file {xmlname} convert success.')
    
    except Exception as e:
        print(f'file {xmlname} convert error.')
        print(f'error message:\n{e}')
        error_file_list.append(xmlname)

if __name__ == "__main__":
    postfix = 'jpg'
    imgpath = 'F:/OneDrive/桌面/my_yolov8/my_yolov8/datasets/substation/test/images'
    xmlpath = 'F:/OneDrive/桌面/my_yolov8/my_yolov8/datasets/substation/test/annotations'
    txtpath = 'F:/OneDrive/桌面/my_yolov8/my_yolov8/datasets/substation/test/labels'
    
    # 打印检查路径
    print(f'Checking paths:')
    print(f'Image path: {imgpath}')
    print(f'XML path: {xmlpath}')
    print(f'TXT path: {txtpath}')
    
    if not os.path.exists(imgpath):
        raise FileNotFoundError(f"Image directory {imgpath} does not exist")
    
    if not os.path.exists(xmlpath):
        raise FileNotFoundError(f"XML directory {xmlpath} does not exist")
    
    if not os.path.exists(txtpath):
        #raise FileNotFoundError(f"TXT directory {txtpath} does not exist")
        os.makedirs(txtpath, exist_ok=True)
    
    file_list = os.listdir(xmlpath)
    error_file_list = []
    
    for filename in file_list:
        file_path = os.path.join(xmlpath, filename)
        if file_path.lower().endswith('.xml'):
            convert_annotation(file_path, filename)
        else:
            print(f'file {filename} is not an XML format.')
    
    print(f'these files failed to convert:\n{error_file_list}')
    print(f'Dataset Classes: {classes}')
