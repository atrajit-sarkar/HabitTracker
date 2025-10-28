"""
MP4 to Lottie Animation Converter with Advanced Optimizations
Converts MP4 videos to optimized Lottie animations with background removal
Features: Frame skipping, duplicate detection, smart compression, background removal
"""

import numpy as np
from PIL import Image
import json
import base64
import io
import os
from pathlib import Path
import sys


def check_dependencies():
    """Check if required dependencies are installed"""
    missing = []
    
    try:
        import cv2
    except ImportError:
        missing.append("opencv-python")
    
    if missing:
        print("\n❌ Missing required dependencies!")
        print(f"Install them with: pip install {' '.join(missing)}")
        print("\nFull installation command:")
        print(f"pip install {' '.join(missing)} pillow numpy")
        return False
    
    # Check for optional rembg (AI background removal)
    try:
        from rembg import remove
        print("✓ rembg available (AI background removal enabled)")
    except ImportError:
        print("ℹ️ rembg not available (AI background removal disabled)")
        print("   Install with: pip install rembg onnxruntime")
        print("   Note: This is optional. Simple background removal will be used.")
    
    return True


def remove_background_rembg(frame):
    """
    Remove background using rembg library (AI-powered)
    
    Args:
        frame: PIL Image in RGBA mode
    
    Returns:
        PIL Image with background removed
    """
    try:
        from rembg import remove
        # Convert PIL image to bytes
        buffer = io.BytesIO()
        frame.save(buffer, format='PNG')
        buffer.seek(0)
        
        # Remove background
        output = remove(buffer.read())
        
        # Convert back to PIL Image
        return Image.open(io.BytesIO(output))
    except Exception as e:
        print(f"Warning: Background removal failed: {e}")
        return frame


def remove_background_simple(frame, dark_threshold=30, white_threshold=200, edge_tolerance=10):
    """
    Simple background removal (fallback method)
    
    Args:
        frame: PIL Image in RGBA mode
        dark_threshold: RGB value below which pixels are considered black background
        white_threshold: RGB value above which pixels are considered white background
        edge_tolerance: Tolerance for edge detection
    
    Returns:
        PIL Image with background removed
    """
    data = np.array(frame)
    
    if data.shape[2] == 3:  # RGB
        alpha = np.ones((data.shape[0], data.shape[1]), dtype=np.uint8) * 255
        data = np.dstack([data, alpha])
    
    r, g, b, a = data[:,:,0], data[:,:,1], data[:,:,2], data[:,:,3]
    
    # Remove BLACK/dark background (this video has black background!)
    black_mask = (r < dark_threshold) & (g < dark_threshold) & (b < dark_threshold)
    
    # Also remove white/light background (just in case)
    white_mask = (r > white_threshold) & (g > white_threshold) & (b > white_threshold)
    
    # Edge detection - preserve edges of the content
    gray = (r.astype(float) + g.astype(float) + b.astype(float)) / 3
    gradient_x = np.abs(np.diff(gray, axis=1, prepend=gray[:,0:1]))
    gradient_y = np.abs(np.diff(gray, axis=0, prepend=gray[0:1,:]))
    gradient = np.sqrt(gradient_x**2 + gradient_y**2)
    
    edge_mask = gradient > edge_tolerance
    
    # Remove background but keep edges/content
    removal_mask = (black_mask | white_mask) & ~edge_mask
    
    data[removal_mask, 3] = 0
    
    return Image.fromarray(data, 'RGBA')


def calculate_frame_difference(frame1, frame2):
    """
    Calculate difference between two frames (0-1, where 0 is identical)
    
    Args:
        frame1: First PIL Image
        frame2: Second PIL Image
    
    Returns:
        Float difference score (0-1)
    """
    # Ensure both frames are the same size
    if frame1.size != frame2.size:
        frame2 = frame2.resize(frame1.size, Image.Resampling.LANCZOS)
    
    arr1 = np.array(frame1.convert('RGB'))
    arr2 = np.array(frame2.convert('RGB'))
    
    diff = np.abs(arr1.astype(float) - arr2.astype(float))
    return np.mean(diff) / 255.0


def optimize_frame(frame, max_size=512):
    """
    Optimize frame for smaller file size
    
    Args:
        frame: PIL Image
        max_size: Maximum dimension
    
    Returns:
        Optimized PIL Image
    """
    if max(frame.size) > max_size:
        ratio = max_size / max(frame.size)
        new_size = (int(frame.size[0] * ratio), int(frame.size[1] * ratio))
        frame = frame.resize(new_size, Image.Resampling.LANCZOS)
    
    return frame


def frame_to_base64(frame, format='WEBP', quality=85):
    """
    Convert PIL Image to base64 string
    
    Args:
        frame: PIL Image
        format: Image format (WEBP, PNG)
        quality: Compression quality (0-100)
    
    Returns:
        Base64 encoded string
    """
    buffer = io.BytesIO()
    
    if format.upper() == 'WEBP':
        frame.save(buffer, format='WEBP', quality=quality, method=6)
        mime_type = 'image/webp'
    else:
        frame.save(buffer, format='PNG', optimize=True)
        mime_type = 'image/png'
    
    buffer.seek(0)
    img_str = base64.b64encode(buffer.read()).decode()
    
    return f"data:{mime_type};base64,{img_str}"


def extract_frames_from_mp4(
    mp4_path,
    remove_bg=True,
    max_size=512,
    target_fps=None,
    skip_frames=1,
    duplicate_threshold=0.02,
    bg_method='simple'
):
    """
    Extract frames from MP4 with advanced optimizations
    
    Args:
        mp4_path: Path to MP4 file
        remove_bg: Whether to remove background
        max_size: Maximum dimension for optimization
        target_fps: Target FPS (None = use original)
        skip_frames: Skip every N frames (1 = use all, 2 = use every 2nd, etc.)
        duplicate_threshold: Threshold for detecting duplicate frames (0-1)
        bg_method: Background removal method ('simple', 'ai', or 'none')
    
    Returns:
        List of (frame, duration_ms) tuples
    """
    try:
        import cv2
    except ImportError:
        print("Error: opencv-python not installed")
        print("Install with: pip install opencv-python")
        return []
    
    print(f"Loading MP4: {mp4_path}")
    
    video = cv2.VideoCapture(mp4_path)
    
    if not video.isOpened():
        print(f"Error: Could not open video file: {mp4_path}")
        return []
    
    # Get video properties
    original_fps = video.get(cv2.CAP_PROP_FPS)
    frame_count = int(video.get(cv2.CAP_PROP_FRAME_COUNT))
    duration_sec = frame_count / original_fps
    
    print(f"Video info: {frame_count} frames, {original_fps:.2f} FPS, {duration_sec:.2f}s")
    
    # Calculate target FPS
    if target_fps is None:
        target_fps = original_fps
    
    # Calculate frame interval
    frame_interval = max(1, int(original_fps / target_fps))
    frame_interval *= skip_frames  # Apply additional skipping
    
    print(f"Extracting every {frame_interval} frame(s) for target {target_fps} FPS")
    
    frames = []
    frame_idx = 0
    prev_frame = None
    duplicates_skipped = 0
    
    while True:
        ret, cv_frame = video.read()
        if not ret:
            break
        
        # Skip frames based on interval
        if frame_idx % frame_interval != 0:
            frame_idx += 1
            continue
        
        # Convert BGR to RGB
        cv_frame = cv2.cvtColor(cv_frame, cv2.COLOR_BGR2RGB)
        
        # Convert to PIL Image
        pil_frame = Image.fromarray(cv_frame)
        pil_frame = pil_frame.convert('RGBA')
        
        # Check for duplicate frames
        if prev_frame is not None and duplicate_threshold > 0:
            diff = calculate_frame_difference(prev_frame, pil_frame)
            if diff < duplicate_threshold:
                duplicates_skipped += 1
                frame_idx += 1
                continue
        
        # Remove background
        if remove_bg:
            if bg_method == 'ai':
                print(f"Processing frame {len(frames) + 1} (AI background removal)...")
                pil_frame = remove_background_rembg(pil_frame)
            elif bg_method == 'simple':
                if len(frames) % 10 == 0:  # Print every 10 frames
                    print(f"Processing frame {len(frames) + 1} (simple background removal)...")
                pil_frame = remove_background_simple(pil_frame)
        else:
            if len(frames) % 10 == 0:
                print(f"Processing frame {len(frames) + 1}...")
        
        # Optimize frame
        pil_frame = optimize_frame(pil_frame, max_size)
        
        # Calculate frame duration
        frame_duration_ms = int(1000 / target_fps)
        
        frames.append((pil_frame, frame_duration_ms))
        prev_frame = pil_frame.copy()
        frame_idx += 1
    
    video.release()
    
    print(f"Extracted {len(frames)} frames (skipped {duplicates_skipped} duplicates)")
    
    return frames


def create_lottie_animation(frames, output_path, fps=None):
    """
    Create Lottie JSON animation from frames
    
    Args:
        frames: List of (frame, duration_ms) tuples
        output_path: Path to save Lottie JSON
        fps: Frames per second (if None, calculated from durations)
    
    Returns:
        Path to created Lottie file
    """
    print("Creating Lottie animation...")
    
    if not frames:
        raise ValueError("No frames to process")
    
    # Get dimensions from first frame
    width, height = frames[0][0].size
    
    # Calculate timing
    total_duration_ms = sum(duration for _, duration in frames)
    
    # Use provided FPS or calculate from frame durations
    if fps is None:
        fps = int(1000 * len(frames) / total_duration_ms)
        fps = max(10, min(fps, 60))  # Clamp between 10-60 fps
    
    print(f"Animation specs: {len(frames)} frames, {fps} FPS, {total_duration_ms}ms duration")
    
    # Convert frames to base64
    print("Encoding frames (this may take a while)...")
    assets = []
    layers = []
    
    for i, (frame, duration) in enumerate(frames):
        if i % 10 == 0:
            print(f"Encoding frame {i + 1}/{len(frames)}...")
        
        # Create asset
        asset_id = f"image_{i}"
        base64_data = frame_to_base64(frame, format='WEBP', quality=85)
        
        assets.append({
            "id": asset_id,
            "w": width,
            "h": height,
            "u": "",
            "p": base64_data,
            "e": 0
        })
        
        # Calculate frame timing
        start_frame = sum(frames[j][1] * fps / 1000 for j in range(i))
        end_frame = start_frame + (duration * fps / 1000)
        
        # Create layer for this frame
        layers.append({
            "ddd": 0,
            "ind": i,
            "ty": 2,  # Image layer
            "nm": f"Frame {i}",
            "refId": asset_id,
            "sr": 1,
            "ks": {
                "o": {
                    "a": 1,
                    "k": [
                        {"t": start_frame, "s": [100], "e": [100]},
                        {"t": end_frame, "s": [100], "e": [0]}
                    ]
                },
                "r": {"a": 0, "k": 0},
                "p": {"a": 0, "k": [width/2, height/2, 0]},
                "a": {"a": 0, "k": [width/2, height/2, 0]},
                "s": {"a": 0, "k": [100, 100, 100]}
            },
            "ao": 0,
            "ip": start_frame,
            "op": end_frame,
            "st": 0,
            "bm": 0
        })
    
    # Create Lottie JSON structure
    lottie_data = {
        "v": "5.7.4",  # Lottie version
        "fr": fps,
        "ip": 0,
        "op": total_duration_ms * fps / 1000,
        "w": width,
        "h": height,
        "nm": "MP4 Animation",
        "ddd": 0,
        "assets": assets,
        "layers": layers,
        "markers": []
    }
    
    # Save to file
    print(f"Saving Lottie animation to: {output_path}")
    with open(output_path, 'w') as f:
        json.dump(lottie_data, f, separators=(',', ':'))
    
    # Get file size
    file_size = os.path.getsize(output_path)
    print(f"✓ Lottie animation created: {file_size / 1024:.2f} KB")
    
    return output_path


def convert_mp4_to_lottie(
    mp4_path,
    output_path=None,
    remove_bg=True,
    max_size=512,
    target_fps=20,
    skip_frames=1,
    duplicate_threshold=0.02,
    bg_method='simple'
):
    """
    Main function to convert MP4 to Lottie animation
    
    Args:
        mp4_path: Path to input MP4 file
        output_path: Path to output Lottie JSON (default: same name as MP4)
        remove_bg: Whether to remove background
        max_size: Maximum dimension for optimization
        target_fps: Target FPS (reduces from original if lower)
        skip_frames: Skip every N frames (1 = use all, 2 = every 2nd, etc.)
        duplicate_threshold: Threshold for detecting duplicate frames (0-1)
        bg_method: Background removal method ('simple', 'ai', or 'none')
    
    Returns:
        Path to created Lottie file
    """
    # Generate output path if not provided
    if output_path is None:
        mp4_name = Path(mp4_path).stem
        output_path = str(Path(mp4_path).parent / f"{mp4_name}_lottie.json")
    
    print("=" * 60)
    print("MP4 TO LOTTIE CONVERTER")
    print("=" * 60)
    print(f"Input: {mp4_path}")
    print(f"Output: {output_path}")
    print(f"Background removal: {bg_method.upper() if remove_bg else 'NO'}")
    print(f"Max size: {max_size}px")
    print(f"Target FPS: {target_fps}")
    print(f"Skip frames: every {skip_frames} frame(s)")
    print(f"Duplicate detection: {'YES' if duplicate_threshold > 0 else 'NO'}")
    print("=" * 60)
    
    # Extract frames
    frames = extract_frames_from_mp4(
        mp4_path,
        remove_bg=remove_bg,
        max_size=max_size,
        target_fps=target_fps,
        skip_frames=skip_frames,
        duplicate_threshold=duplicate_threshold,
        bg_method=bg_method if remove_bg else 'none'
    )
    
    if not frames:
        print("❌ No frames extracted. Conversion failed.")
        return None
    
    # Create Lottie animation
    result = create_lottie_animation(frames, output_path, fps=target_fps)
    
    print("=" * 60)
    print("✓ CONVERSION COMPLETE!")
    print("=" * 60)
    
    return result


if __name__ == "__main__":
    # Check dependencies
    if not check_dependencies():
        sys.exit(1)
    
    # Example usage with different optimization levels
    print("\n🎬 MP4 to Lottie Converter")
    print("=" * 60)
    
    # Check if MP4 file provided as argument
    if len(sys.argv) > 1:
        mp4_file = sys.argv[1]
        output_file = sys.argv[2] if len(sys.argv) > 2 else None
        
        if not os.path.exists(mp4_file):
            print(f"❌ Error: File not found: {mp4_file}")
            sys.exit(1)
        
        # Convert with default settings
        convert_mp4_to_lottie(
            mp4_path=mp4_file,
            output_path=output_file,
            remove_bg=True,
            max_size=256,
            target_fps=20,
            skip_frames=1,
            duplicate_threshold=0.02,
            bg_method='simple'  # Use 'ai' for better results (slower)
        )
    else:
        print("\n📝 Usage Examples:")
        print("-" * 60)
        print("\n1. Basic conversion (recommended settings):")
        print("   python mp4_to_lottie.py video.mp4")
        print("\n2. Specify output file:")
        print("   python mp4_to_lottie.py video.mp4 output.json")
        print("\n3. Custom settings in Python:")
        print("""
from mp4_to_lottie import convert_mp4_to_lottie

# High quality (larger file)
convert_mp4_to_lottie(
    mp4_path="video.mp4",
    output_path="output_hq.json",
    remove_bg=True,
    max_size=512,
    target_fps=30,
    skip_frames=1,
    duplicate_threshold=0.01,
    bg_method='ai'  # AI-powered (requires rembg)
)

# Optimized (smaller file)
convert_mp4_to_lottie(
    mp4_path="video.mp4",
    output_path="output_optimized.json",
    remove_bg=True,
    max_size=256,
    target_fps=15,
    skip_frames=2,  # Use every 2nd frame
    duplicate_threshold=0.05,
    bg_method='simple'
)

# No background removal (fastest)
convert_mp4_to_lottie(
    mp4_path="video.mp4",
    output_path="output_fast.json",
    remove_bg=False,
    max_size=256,
    target_fps=20,
    skip_frames=1
)
""")
        print("-" * 60)
        print("\n💡 Tips for Best Results:")
        print("  • Use short clips (1-5 seconds)")
        print("  • Lower FPS (15-20) for smaller files")
        print("  • max_size=256 for mobile apps")
        print("  • skip_frames=2 cuts file size by ~50%")
        print("  • duplicate_threshold=0.05 removes similar frames")
        print("  • bg_method='ai' for best background removal (slower)")
        print("  • bg_method='simple' for faster processing")
        print("\n📦 Install dependencies:")
        print("  pip install opencv-python pillow numpy")
        print("  pip install rembg  # Optional, for AI background removal")
        print("=" * 60)
