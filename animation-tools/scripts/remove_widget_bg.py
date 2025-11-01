"""
Remove background from widget PNG images using AI (rembg)
"""
import sys
import os
from pathlib import Path
from PIL import Image

try:
    from rembg import remove
    REMBG_AVAILABLE = True
except ImportError:
    REMBG_AVAILABLE = False
    print("⚠️  rembg not available. Install with: pip install rembg")

def remove_background_ai(input_path, output_path):
    """
    Remove background using AI model (rembg)
    
    Args:
        input_path: Path to input PNG
        output_path: Path to output PNG
    """
    if not REMBG_AVAILABLE:
        print(f"❌ Cannot process {input_path} - rembg not installed")
        return False
    
    print(f"🤖 AI Processing: {input_path}")
    
    try:
        # Open image
        with open(input_path, 'rb') as input_file:
            input_data = input_file.read()
        
        # Remove background using AI
        output_data = remove(input_data)
        
        # Save result
        with open(output_path, 'wb') as output_file:
            output_file.write(output_data)
        
        print(f"✅ Saved: {output_path}")
        return True
    except Exception as e:
        print(f"❌ Error processing {input_path}: {e}")
        return False

def process_widget_assets():
    """Process all widget asset PNG files"""
    script_dir = Path(__file__).parent.parent.parent  # Go up to project root
    input_dir = script_dir / 'widgets-assets'
    output_dir = script_dir / 'app' / 'src' / 'main' / 'res' / 'drawable'
    
    # Create output directory if it doesn't exist
    output_dir.mkdir(parents=True, exist_ok=True)
    
    # Process each PNG file
    png_files = [
        '1-overdue.png',
        'all-done.png',
        'more-overdue.png',
        'no-overdue.png'
    ]
    
    success_count = 0
    for filename in png_files:
        input_path = input_dir / filename
        if input_path.exists():
            # Convert filename to Android resource naming (lowercase, no hyphens)
            output_name = f"widget_{filename.replace('-', '_')}"
            output_path = output_dir / output_name
            
            if remove_background_ai(str(input_path), str(output_path)):
                success_count += 1
        else:
            print(f"❌ File not found: {input_path}")
    
    print(f"\n✨ Processed {success_count}/{len(png_files)} images successfully!")

if __name__ == '__main__':
    print("🎨 Removing backgrounds from widget assets using AI...")
    process_widget_assets()
    print("✅ Done!")
