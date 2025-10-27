"""
Mangekyo Sharingan Animation from Image (Universal)
Takes any Sharingan PNG/SVG image, removes background, creates rotating animation, converts to Lottie
Works with any Mangekyo pattern: Itachi, Kakashi, Sasuke, Obito, etc.
"""

import numpy as np
from PIL import Image, ImageOps, ImageDraw
import json
import base64
import io


def load_and_process_image(image_path):
    """Load image and remove background"""
    print(f"Loading image: {image_path}")
    
    # Load image
    img = Image.open(image_path)
    
    # Convert to RGBA if not already
    if img.mode != 'RGBA':
        img = img.convert('RGBA')
    
    print(f"Original size: {img.size}")
    
    # Get image data
    data = np.array(img)
    
    # Remove white/light background
    # Create mask for white-ish pixels
    r, g, b, a = data[:,:,0], data[:,:,1], data[:,:,2], data[:,:,3]
    
    # Pixels that are very light (close to white) become transparent
    white_threshold = 200
    white_mask = (r > white_threshold) & (g > white_threshold) & (b > white_threshold)
    
    # Set alpha to 0 for white pixels
    data[white_mask, 3] = 0
    
    # Create clean image
    clean_img = Image.fromarray(data, 'RGBA')
    
    # Resize to 512x512 maintaining aspect ratio with padding
    target_size = 512
    if clean_img.size != (target_size, target_size):
        print(f"Resizing to {target_size}x{target_size} (maintaining aspect ratio)...")
        
        # Calculate scaling to fit inside target size while maintaining aspect ratio
        width, height = clean_img.size
        scale = min(target_size / width, target_size / height)
        new_width = int(width * scale)
        new_height = int(height * scale)
        
        # Resize maintaining aspect ratio
        clean_img = clean_img.resize((new_width, new_height), Image.LANCZOS)
        
        # Create a new square image with transparent background
        square_img = Image.new('RGBA', (target_size, target_size), (0, 0, 0, 0))
        
        # Paste the resized image in the center
        paste_x = (target_size - new_width) // 2
        paste_y = (target_size - new_height) // 2
        square_img.paste(clean_img, (paste_x, paste_y), clean_img)
        
        clean_img = square_img
    
    print(f"✓ Image processed - Background removed")
    return clean_img


def image_to_base64(img):
    """Convert PIL Image to base64 string"""
    buffered = io.BytesIO()
    img.save(buffered, format="PNG")
    img_str = base64.b64encode(buffered.getvalue()).decode()
    return img_str


def create_rotating_lottie_with_image(img, character_name="Sharingan"):
    """Create Lottie JSON with embedded rotating image"""
    print(f"\nCreating Lottie animation with rotating {character_name}...")
    
    # Convert image to base64
    img_base64 = image_to_base64(img)
    
    # Create unique asset ID
    asset_id = f"{character_name.lower()}_sharingan_img"
    
    size = 512
    frames = 90  # 3 seconds at 30fps
    center = size // 2
    
    lottie = {
        "v": "5.9.0",
        "fr": 30,
        "ip": 0,
        "op": frames,
        "w": size,
        "h": size,
        "nm": f"{character_name} Mangekyo Sharingan",
        "ddd": 0,
        "assets": [
            {
                "id": asset_id,
                "w": size,
                "h": size,
                "u": "",
                "p": f"data:image/png;base64,{img_base64}",
                "e": 0
            }
        ],
        "layers": [
            {
                "ddd": 0,
                "ind": 1,
                "ty": 2,  # Image layer
                "nm": f"Rotating {character_name} Sharingan",
                "refId": asset_id,
                "sr": 1,
                "ks": {
                    "o": {"a": 0, "k": 100},
                    "r": {
                        "a": 1,
                        "k": [
                            {
                                "i": {"x": [0.667], "y": [1]},
                                "o": {"x": [0.333], "y": [0]},
                                "t": 0,
                                "s": [0]
                            },
                            {
                                "t": frames,
                                "s": [360]
                            }
                        ]
                    },
                    "p": {"a": 0, "k": [center, center, 0]},
                    "a": {"a": 0, "k": [center, center, 0]},
                    "s": {"a": 0, "k": [100, 100, 100]}
                },
                "ao": 0,
                "ip": 0,
                "op": frames,
                "st": 0,
                "bm": 0
            }
        ],
        "markers": []
    }
    
    return lottie


def create_preview_gif(img, output_path):
    """Create animated GIF preview"""
    print("\nCreating preview GIF...")
    
    frames = []
    num_frames = 90
    
    for i in range(num_frames):
        angle = (i / num_frames) * 360
        rotated = img.rotate(-angle, resample=Image.BICUBIC, expand=False)
        
        # Resize for smaller GIF
        preview_size = 256
        small = rotated.resize((preview_size, preview_size), Image.LANCZOS)
        frames.append(small)
    
    # Save as GIF
    frames[0].save(
        output_path,
        save_all=True,
        append_images=frames[1:],
        duration=1000//30,  # 30 FPS
        loop=0,
        optimize=True,
        transparency=0
    )
    
    import os
    size_kb = os.path.getsize(output_path) / 1024
    print(f"✓ Preview GIF saved: {output_path} ({size_kb:.2f} KB)")


def main():
    import sys
    
    print("=" * 70)
    print("🔥 MANGEKYO SHARINGAN ANIMATION - FROM IMAGE 🔥")
    print("=" * 70)
    print()
    
    # Check for command line argument
    if len(sys.argv) > 1:
        image_path = sys.argv[1]
        character_name = sys.argv[2] if len(sys.argv) > 2 else "Custom"
    else:
        # Default to Kakashi image
        image_path = "Mangekyou_Sharingan_Kakashi.svg.png"
        character_name = "Kakashi"
    
    print(f"Character: {character_name}")
    print(f"Image: {image_path}")
    print()
    
    # Load and process image
    img = load_and_process_image(image_path)
    
    # Generate output filenames
    char_lower = character_name.lower().replace(" ", "_")
    processed_image = f"{char_lower}_processed.png"
    preview_gif = f"{char_lower}_sharingan_preview.gif"
    output_json = f"mangekyo_{char_lower}.json"
    
    # Save processed image for preview
    img.save(processed_image)
    print(f"✓ Saved processed image: {processed_image}")
    
    # Create preview GIF
    create_preview_gif(img, preview_gif)
    
    # Create Lottie JSON
    lottie_data = create_rotating_lottie_with_image(img, character_name)
    
    # Save Lottie JSON
    with open(output_json, 'w') as f:
        json.dump(lottie_data, f, separators=(',', ':'))
    
    import os
    size_kb = os.path.getsize(output_json) / 1024
    print(f"\n✓ Lottie JSON saved: {output_json} ({size_kb:.2f} KB)")
    
    print("\n" + "=" * 70)
    print(f"✅ {character_name.upper()} MANGEKYO COMPLETE!")
    print("=" * 70)
    print("\nGenerated files:")
    print(f"  📄 {output_json} - Lottie animation with embedded image")
    print(f"  🎬 {preview_gif} - Preview animation")
    print(f"  🖼️  {processed_image} - Processed image (transparent bg)")
    print("\nFeatures:")
    print(f"  ✓ Original {character_name} Mangekyo design from image")
    print("  ✓ Background removed (transparent)")
    print("  ✓ Smooth 360° rotation")
    print("  ✓ Ready for Android app")
    print("\nUsage:")
    print(f"  # Use default (Kakashi)")
    print(f"  python create_sharingan_from_image.py")
    print()
    print(f"  # Use custom image")
    print(f"  python create_sharingan_from_image.py <image_path> <character_name>")
    print()
    print("Next steps:")
    print(f"  Copy-Item {output_json} app/src/main/assets/animations/mangekyo_itachi.json -Force")
    print("  .\\gradlew.bat installPlaystoreDebug")
    print()


if __name__ == "__main__":
    main()
