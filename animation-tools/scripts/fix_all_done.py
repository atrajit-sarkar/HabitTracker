"""
Fix all-done.png specifically - remove BLACK background while preserving character
"""
from pathlib import Path
from PIL import Image
import numpy as np

def remove_black_background(input_path, output_path):
    """
    Remove black background while preserving ALL character details
    Uses smart detection to only remove pure black background
    """
    print(f"🎨 Processing: {input_path}")
    
    # Open image
    img = Image.open(input_path).convert('RGBA')
    data = np.array(img)
    
    r, g, b, a = data[:,:,0], data[:,:,1], data[:,:,2], data[:,:,3]
    
    # Detect pure black or very dark pixels (likely background)
    # Be conservative - only remove very dark pixels
    dark_threshold = 15  # Only very dark pixels
    
    # Create mask for dark pixels
    dark_mask = (r < dark_threshold) & (g < dark_threshold) & (b < dark_threshold)
    
    # Calculate edge detection to preserve anti-aliasing
    gray = (r.astype(float) + g.astype(float) + b.astype(float)) / 3
    gradient_x = np.abs(np.diff(gray, axis=1, prepend=gray[:,0:1]))
    gradient_y = np.abs(np.diff(gray, axis=0, prepend=gray[0:1,:]))
    gradient = np.sqrt(gradient_x**2 + gradient_y**2)
    
    # Don't remove pixels near edges (preserve anti-aliasing)
    edge_mask = gradient > 5
    
    # Only remove dark pixels that are NOT near edges
    removal_mask = dark_mask & ~edge_mask
    
    # Set alpha to 0 for background pixels
    data[removal_mask, 3] = 0
    
    # Also reduce alpha slightly for semi-dark edge pixels (smooth transition)
    semi_dark_mask = (r < dark_threshold * 2) & (g < dark_threshold * 2) & (b < dark_threshold * 2) & edge_mask
    data[semi_dark_mask, 3] = (data[semi_dark_mask, 3] * 0.3).astype(np.uint8)
    
    # Save result
    result = Image.fromarray(data, 'RGBA')
    result.save(output_path, 'PNG', optimize=True)
    print(f"✅ Saved: {output_path}")
    print(f"✨ Black background removed, character preserved!")

def process_all_done():
    """Process only all-done.png to remove black background"""
    script_dir = Path(__file__).parent.parent.parent
    input_dir = script_dir / 'widgets-assets'
    output_dir = script_dir / 'app' / 'src' / 'main' / 'res' / 'drawable'
    
    input_path = input_dir / 'all-done.png'
    output_path = output_dir / 'widget_all_done.png'
    
    if input_path.exists():
        remove_black_background(str(input_path), str(output_path))
    else:
        print(f"❌ File not found: {input_path}")

if __name__ == '__main__':
    print("🔧 Removing BLACK background from all-done.png...")
    process_all_done()
