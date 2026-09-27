import math
import os
import subprocess
from PIL import Image, ImageDraw, ImageFont

def render_frame(frame_num, total_frames, t):
    width, height = 720, 1280
    im = Image.new("RGB", (width, height), (228, 226, 210))
    draw = ImageDraw.Draw(im)

    # 1. Background wall gradient
    for y in range(height):
        factor = y / height
        r = int(235 - factor * 25)
        g = int(236 - factor * 24)
        b = int(218 - factor * 25)
        draw.line([(0, y), (width, y)], fill=(r, g, b))

    # Wall corner shadow on left (slanted wall corner from video)
    for x in range(160):
        alpha = int(40 * (1 - x / 160))
        draw.line([(x, 0), (x, height)], fill=(200 - alpha, 205 - alpha, 195 - alpha))

    # Dark curtain / doorway edge on bottom left
    draw.polygon([(0, 750), (140, 800), (110, height), (0, height)], fill=(32, 45, 75))

    # 2. Movement / nod / breathing
    head_nod = math.sin(t * 3.5) * 8.0
    mouth_phase = abs(math.sin(t * 7.2))

    # Center of head
    cx = width / 2 + 10
    cy = 650 + head_nod

    # 3. Shoulders / Tan Crewneck T-shirt (#C6AC94)
    shirt_color = (198, 172, 148)
    collar_color = (180, 155, 132)

    # Large torso
    draw.polygon([
        (cx - 280, height),
        (cx - 240, cy + 300),
        (cx - 150, cy + 220),
        (cx, cy + 210),
        (cx + 150, cy + 220),
        (cx + 240, cy + 300),
        (cx + 280, height)
    ], fill=shirt_color)
    draw.ellipse([cx - 260, cy + 200, cx + 260, cy + 420], fill=shirt_color)

    # Collar line
    draw.ellipse([cx - 90, cy + 200, cx + 90, cy + 250], outline=collar_color, width=8)

    # 4. Neck
    neck_color = (185, 126, 85)
    draw.rectangle([cx - 75, cy + 80, cx + 75, cy + 220], fill=neck_color)

    # 5. Ears
    ear_color = (195, 132, 90)
    draw.ellipse([cx - 195, cy - 60, cx - 145, cy + 50], fill=ear_color)
    draw.ellipse([cx + 145, cy - 60, cx + 195, cy + 50], fill=ear_color)

    # 6. Bald Head & Face Shape (Dome)
    skin_base = (214, 152, 107)
    draw.ellipse([cx - 170, cy - 250, cx + 170, cy + 120], fill=skin_base)

    # Forehead highlight
    draw.ellipse([cx - 100, cy - 230, cx + 100, cy - 170], fill=(235, 175, 130))

    # 7. Eyebrows
    draw.arc([cx - 130, cy - 90, cx - 30, cy - 40], start=200, end=350, fill=(20, 20, 20), width=14)
    draw.arc([cx + 30, cy - 90, cx + 130, cy - 40], start=190, end=340, fill=(20, 20, 20), width=14)

    # 8. Eyes
    draw.ellipse([cx - 110, cy - 65, cx - 45, cy - 35], fill=(255, 255, 255))
    draw.ellipse([cx - 88, cy - 60, cx - 62, cy - 38], fill=(30, 25, 20))
    draw.ellipse([cx - 80, cy - 56, cx - 74, cy - 50], fill=(255, 255, 255))

    draw.ellipse([cx + 45, cy - 65, cx + 110, cy - 35], fill=(255, 255, 255))
    draw.ellipse([cx + 62, cy - 60, cx + 88, cy - 38], fill=(30, 25, 20))
    draw.ellipse([cx + 70, cy - 56, cx + 76, cy - 50], fill=(255, 255, 255))

    # 9. Nose
    draw.ellipse([cx - 30, cy - 30, cx + 30, cy + 20], fill=(195, 130, 85))

    # 10. Full dense black beard covering lower jaw & chin
    beard_color = (20, 20, 22)
    draw.polygon([
        (cx - 170, cy - 20),
        (cx - 180, cy + 90),
        (cx - 140, cy + 210),
        (cx, cy + 235),
        (cx + 140, cy + 210),
        (cx + 180, cy + 90),
        (cx + 170, cy - 20),
        (cx + 120, cy + 50),
        (cx, cy + 135),
        (cx - 120, cy + 50)
    ], fill=beard_color)
    draw.ellipse([cx - 140, cy + 80, cx + 140, cy + 235], fill=beard_color)

    # 11. Mouth (Moving/Talking to the chant)
    mouth_open_h = int(12 + mouth_phase * 35)
    draw.ellipse([cx - 50, cy + 55, cx + 50, cy + 55 + mouth_open_h], fill=(60, 15, 20))
    # Teeth
    draw.rectangle([cx - 32, cy + 57, cx + 32, cy + 65], fill=(245, 245, 245))
    # Tongue
    draw.ellipse([cx - 25, cy + 62 + mouth_open_h // 2, cx + 25, cy + 55 + mouth_open_h], fill=(180, 50, 60))

    # Mustache
    draw.arc([cx - 85, cy + 15, cx + 85, cy + 65], start=10, end=170, fill=beard_color, width=28)
    draw.ellipse([cx - 75, cy + 30, cx + 75, cy + 65], fill=beard_color)

    # 12. Thick Black Rectangular Glasses
    glasses_color = (15, 15, 18)
    gl_w, gl_h = 105, 68
    # Left lens frame
    draw.rounded_rectangle([cx - 135, cy - 75, cx - 135 + gl_w, cy - 75 + gl_h], radius=14, outline=glasses_color, width=10)
    # Right lens frame
    draw.rounded_rectangle([cx + 30, cy - 75, cx + 30 + gl_w, cy - 75 + gl_h], radius=14, outline=glasses_color, width=10)
    # Bridge
    draw.line([(cx - 30, cy - 50), (cx + 30, cy - 50)], fill=glasses_color, width=9)
    # Temples to ears
    draw.line([(cx - 135, cy - 55), (cx - 170, cy - 40)], fill=glasses_color, width=8)
    draw.line([(cx + 135, cy - 55), (cx + 170, cy - 40)], fill=glasses_color, width=8)

    # Lens reflection glint
    glint_x = (t * 80) % 180
    draw.line([(cx - 110 + glint_x * 0.4, cy - 65), (cx - 90 + glint_x * 0.4, cy - 25)], fill=(255, 255, 255, 140), width=4)
    draw.line([(cx + 55 + glint_x * 0.4, cy - 65), (cx + 75 + glint_x * 0.4, cy - 25)], fill=(255, 255, 255, 140), width=4)

    # 13. Video filter sparkle in bottom right
    sparkle_x, sparkle_y = width - 80, height - 120
    draw.polygon([
        (sparkle_x, sparkle_y - 20),
        (sparkle_x + 5, sparkle_y - 5),
        (sparkle_x + 20, sparkle_y),
        (sparkle_x + 5, sparkle_y + 5),
        (sparkle_x, sparkle_y + 20),
        (sparkle_x - 5, sparkle_y + 5),
        (sparkle_x - 20, sparkle_y),
        (sparkle_x - 5, sparkle_y - 5)
    ], fill=(255, 215, 0))

    return im

def main():
    os.makedirs("/tmp/frames", exist_ok=True)
    fps = 24
    duration = 8.5
    total_frames = int(fps * duration)
    print(f"Rendering {total_frames} frames...")

    for i in range(total_frames):
        t = i / fps
        im = render_frame(i, total_frames, t)
        im.save(f"/tmp/frames/frame_{i:04d}.png")

    print("Encoding video with ffmpeg...")
    cmd = [
        "ffmpeg", "-y",
        "-r", str(fps),
        "-i", "/tmp/frames/frame_%04d.png",
        "-i", "/tmp/chant_de.wav",
        "-c:v", "libx264",
        "-pix_fmt", "yuv420p",
        "-c:a", "aac",
        "-b:a", "128k",
        "-shortest",
        "/app/applet/app/src/main/res/raw/loading_video.mp4"
    ]
    subprocess.run(cmd, check=True)
    print("Done! Video successfully created at /app/applet/app/src/main/res/raw/loading_video.mp4")

if __name__ == "__main__":
    main()
