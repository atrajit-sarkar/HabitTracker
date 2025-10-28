"""
GIF to Lottie Animation Converter with Background Removal
Converts GIF files to optimized Lottie animations with transparent background
Preserves original timing and visual data
"""

import numpy as np
from PIL import Image
import json
import base64
import io
import os
from pathlib import Path


def remove_background(frame, threshold=200, edge_tolerance=10):
    """
    Remove background from a frame using advanced edge detection
    
    Args:
        frame: PIL Image in RGBA mode
        threshold: RGB value above which pixels are considered background (0-255)
        edge_tolerance: Tolerance for edge detection to preserve anti-aliasing
    
    Returns:
        PIL Image with background removed
    """
    # Convert to numpy array
    data = np.array(frame)
    
    if data.shape[2] == 3:  # RGB
        # Add alpha channel
        alpha = np.ones((data.shape[0], data.shape[1]), dtype=np.uint8) * 255
        data = np.dstack([data, alpha])
    
    r, g, b, a = data[:,:,0], data[:,:,1], data[:,:,2], data[:,:,3]
    
    # Method 1: Remove white/light background
    white_mask = (r > threshold) & (g > threshold) & (b > threshold)
    
    # Method 2: Edge-preserving detection (preserve anti-aliased edges)
    # Calculate gradient to detect edges
    gray = (r.astype(float) + g.astype(float) + b.astype(float)) / 3
    gradient_x = np.abs(np.diff(gray, axis=1, prepend=gray[:,0:1]))
    gradient_y = np.abs(np.diff(gray, axis=0, prepend=gray[0:1,:]))
    gradient = np.sqrt(gradient_x**2 + gradient_y**2)
    
    # Don't remove pixels near edges (they might be anti-aliased)
    edge_mask = gradient > edge_tolerance
    
    # Combine masks: remove white pixels that are not near edges
    removal_mask = white_mask & ~edge_mask
    
    # Set alpha to 0 for background pixels
    data[removal_mask, 3] = 0
    
    return Image.fromarray(data, 'RGBA')


def optimize_frame(frame, max_size=512, quality=90):
    """
    Optimize frame for smaller file size while maintaining quality
    
    Args:
        frame: PIL Image
        max_size: Maximum dimension (width or height)
        quality: JPEG quality for compression (0-100)
    
    Returns:
        Optimized PIL Image
    """
    # Resize if needed
    if max(frame.size) > max_size:
        ratio = max_size / max(frame.size)
        new_size = (int(frame.size[0] * ratio), int(frame.size[1] * ratio))
        frame = frame.resize(new_size, Image.Resampling.LANCZOS)
    
    return frame


def frame_to_base64(frame, format='PNG'):
    """
    Convert PIL Image to base64 string
    
    Args:
        frame: PIL Image
        format: Image format (PNG, WEBP)
    
    Returns:
        Base64 encoded string
    """
    buffer = io.BytesIO()
    
    if format.upper() == 'WEBP':
        # WebP provides better compression
        frame.save(buffer, format='WEBP', quality=90, method=6)
        mime_type = 'image/webp'
    else:
        # PNG for lossless quality
        frame.save(buffer, format='PNG', optimize=True)
        mime_type = 'image/png'
    
    buffer.seek(0)
    img_str = base64.b64encode(buffer.read()).decode()
    
    return f"data:{mime_type};base64,{img_str}"


def extract_frames_from_gif(gif_path, remove_bg=True, max_size=512):
    """
    Extract all frames from GIF with optional background removal
    
    Args:
        gif_path: Path to GIF file
        remove_bg: Whether to remove background
        max_size: Maximum dimension for optimization
    
    Returns:
        List of (frame, duration_ms) tuples
    """
    print(f"Loading GIF: {gif_path}")
    
    gif = Image.open(gif_path)
    frames = []
    
    try:
        frame_count = 0
        while True:
            # Get current frame
            frame = gif.copy().convert('RGBA')
            
            # Get frame duration (in milliseconds)
            duration = gif.info.get('duration', 100)  # Default 100ms
            
            # Remove background if requested
            if remove_bg:
                print(f"Processing frame {frame_count + 1} (removing background)...")
                frame = remove_background(frame)
            
            # Optimize frame
            frame = optimize_frame(frame, max_size)
            
            frames.append((frame, duration))
            frame_count += 1
            
            # Move to next frame
            gif.seek(gif.tell() + 1)
            
    except EOFError:
        pass  # End of frames
    
    print(f"Extracted {len(frames)} frames")
    
    return frames


def create_lottie_animation(frames, output_path, fps=None, loop=True):
    """
    Create Lottie JSON animation from frames
    
    Args:
        frames: List of (frame, duration_ms) tuples
        output_path: Path to save Lottie JSON
        fps: Frames per second (if None, uses GIF timing)
        loop: Whether animation should loop
    
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
    
    # Use provided FPS or calculate from GIF timing
    if fps is None:
        fps = int(1000 * len(frames) / total_duration_ms)
        fps = max(10, min(fps, 60))  # Clamp between 10-60 fps
    
    print(f"Animation specs: {len(frames)} frames, {fps} fps, {total_duration_ms}ms duration")
    
    # Convert frames to base64
    print("Encoding frames...")
    assets = []
    layers = []
    
    for i, (frame, duration) in enumerate(frames):
        # Create asset
        asset_id = f"image_{i}"
        base64_data = frame_to_base64(frame, format='WEBP')
        
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
        "nm": "GIF Animation",
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


def convert_gif_to_lottie(gif_path, output_path=None, remove_bg=True, max_size=512, fps=None):
    """
    Main function to convert GIF to Lottie animation
    
    Args:
        gif_path: Path to input GIF file
        output_path: Path to output Lottie JSON (default: same name as GIF)
        remove_bg: Whether to remove background
        max_size: Maximum dimension for optimization
        fps: Frames per second (if None, uses GIF timing)
    
    Returns:
        Path to created Lottie file
    """
    # Generate output path if not provided
    if output_path is None:
        gif_name = Path(gif_path).stem
        output_path = str(Path(gif_path).parent / f"{gif_name}_lottie.json")
    
    print("=" * 60)
    print("GIF TO LOTTIE CONVERTER")
    print("=" * 60)
    print(f"Input: {gif_path}")
    print(f"Output: {output_path}")
    print(f"Background removal: {'YES' if remove_bg else 'NO'}")
    print(f"Max size: {max_size}px")
    print("=" * 60)
    
    # Extract frames
    frames = extract_frames_from_gif(gif_path, remove_bg=remove_bg, max_size=max_size)
    
    # Create Lottie animation
    result = create_lottie_animation(frames, output_path, fps=fps)
    
    print("=" * 60)
    print("✓ CONVERSION COMPLETE!")
    print("=" * 60)
    
    return result


if __name__ == "__main__":
    # Default: Convert laughing.gif in gifassests folder
    script_dir = Path(__file__).parent
    gif_path = script_dir / "gifassests" / "laughing.gif"
    output_path = script_dir / "animations" / "laughing_lottie.json"
    
    # Ensure output directory exists
    output_path.parent.mkdir(exist_ok=True)
    
    if not gif_path.exists():
        print(f"Error: GIF file not found: {gif_path}")
        print("\nUsage:")
        print("  python gif_to_lottie.py")
        print("  or")
        print("  python gif_to_lottie.py <gif_path> <output_path>")
    else:
        # Convert with background removal
        convert_gif_to_lottie(
            str(gif_path),
            str(output_path),
            remove_bg=True,
            max_size=512,
            fps=None  # Auto-detect from GIF
        )
        
        print(f"\n✓ You can now use this animation in your Android app!")
        print(f"  Location: {output_path}")
