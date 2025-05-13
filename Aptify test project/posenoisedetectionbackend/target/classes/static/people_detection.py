from ultralytics import YOLO
import cv2
import numpy as np
import mediapipe as mp
import sys
import time

try:
    model = YOLO("yolov8n-pose.pt")
except Exception as e:
    print(f"Error loading YOLO model: {e}")
    sys.exit(1)

mp_face_mesh = mp.solutions.face_mesh
mp_drawing = mp.solutions.drawing_utils
face_mesh = mp_face_mesh.FaceMesh(max_num_faces=5, refine_landmarks=True,
                                  min_detection_confidence=0.5, min_tracking_confidence=0.5)
face_specs = mp_drawing.DrawingSpec(color=(0, 255, 0), thickness=1)

cap = cv2.VideoCapture(0)
if not cap.isOpened():
    print("Error: Could not open camera.")
    sys.exit(1)
cap.set(cv2.CAP_PROP_FRAME_WIDTH, 640)
cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)

output_path = "live_output.jpg" if len(sys.argv) < 2 else sys.argv[1]

try:
    while True:
        ret, frame = cap.read()
        if not ret:
            print("Error: Failed to capture image.")
            break

        frame = cv2.flip(frame, 1)
        frame_rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
        results = model(frame, conf=0.5)
        annotated_frame = frame.copy()
        num_people = len(results[0].boxes)

        for result in results:
            boxes = result.boxes.xyxy.cpu().numpy()
            keypoints = result.keypoints.xy.cpu().numpy()
            confidences = result.boxes.conf.cpu().numpy()

            for i in range(len(boxes)):
                x1, y1, x2, y2 = map(int, boxes[i])
                conf = confidences[i]
                cv2.rectangle(annotated_frame, (x1, y1), (x2, y2), (0, 255, 0), 2)
                label = f"Person {conf:.2f}"
                text_size = cv2.getTextSize(label, cv2.FONT_HERSHEY_SIMPLEX, 0.5, 1)[0]
                cv2.rectangle(annotated_frame, (x1, y1 - text_size[1] - 5),
                              (x1 + text_size[0], y1), (0, 255, 0), -1)
                cv2.putText(annotated_frame, label, (x1, y1 - 5),
                            cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 0, 0), 1, cv2.LINE_AA)

                kpts = keypoints[i]
                body_connections = [(5, 6), (5, 7), (7, 9), (6, 8), (8, 10),
                                    (5, 11), (6, 12), (11, 12),
                                    (11, 13), (13, 15), (12, 14), (14, 16)]
                for idx, (x, y) in enumerate(kpts):
                    if x > 0 and y > 0 and idx >= 5:
                        cv2.circle(annotated_frame, (int(x), int(y)), 5, (255, 0, 0), -1)
                for start, end in body_connections:
                    if kpts[start].sum() > 0 and kpts[end].sum() > 0:
                        cv2.line(annotated_frame, (int(kpts[start][0]), int(kpts[start][1])),
                                 (int(kpts[end][0]), int(kpts[end][1])), (255, 0, 0), 2)

        face_results = face_mesh.process(frame_rgb)
        if face_results.multi_face_landmarks:
            for face_landmarks in face_results.multi_face_landmarks:
                mp_drawing.draw_landmarks(
                    image=annotated_frame,
                    landmark_list=face_landmarks,
                    connections=mp_face_mesh.FACEMESH_TESSELATION,
                    landmark_drawing_spec=None,
                    connection_drawing_spec=face_specs
                )

        if num_people > 1:
            text = f"WARNING: {num_people} People Detected!"
            text_size = cv2.getTextSize(text, cv2.FONT_HERSHEY_SIMPLEX, 1, 2)[0]
            text_x = annotated_frame.shape[1] - text_size[0] - 30
            cv2.putText(annotated_frame, text, (text_x, 60),
                        cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 0, 255), 2, cv2.LINE_AA)

        if not cv2.imwrite(output_path, annotated_frame):
            print(f"Error: Failed to write image to {output_path}")
            break

        print(f"People detected: {num_people}", flush=True)
        time.sleep(0.033)  # ~30 FPS

except Exception as e:
    print(f"Error processing frame: {e}")
finally:
    cap.release()
    face_mesh.close()