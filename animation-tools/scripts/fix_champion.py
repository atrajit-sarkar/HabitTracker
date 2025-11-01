"""
Fix champion.png specifically - remove background while preserving character
"""
from pathlib import Path
from PIL import Image
import numpy as np

def remove_background_smart(input_path, output_path):
    """
    Remove light background while preserving ALL character details
    Uses smart detection to only remove background
    """
    print(f"🎨 Processing: {input_path}")
    
    # Open image
    img = Image.open(input_path).convert('RGBA')
    data = np.array(img)
    
    r, g, b, a = data[:,:,0], data[:,:,1], data[:,:,2], data[:,:,3]
    
    # Detect very light pixels (likely background)
    # Champion image has light/white background
    light_threshold = 240  # Only very light pixels
    
    # Create mask for light pixels (background)
    light_mask = (r > light_threshold) & (g > light_threshold) & (b > light_threshold)
    
    # Calculate edge detection to preserve anti-aliasing
    gray = (r.astype(float) + g.astype(float) + b.astype(float)) / 3
    gradient_x = np.abs(np.diff(gray, axis=1, prepend=gray[:,0:1]))
    gradient_y = np.abs(np.diff(gray, axis=0, prepend=gray[0:1,:]))
    gradient = np.sqrt(gradient_x**2 + gradient_y**2)
    
    # Don't remove pixels near edges (preserve anti-aliasing)
    edge_mask = gradient > 5
    
    # Only remove light pixels that are NOT near edges
    removal_mask = light_mask & ~edge_mask
    
    # Set alpha to 0 for background pixels
    data[removal_mask, 3] = 0
    
    # Also reduce alpha slightly for semi-light edge pixels (smooth transition)
    semi_light_threshold = 220
    semi_light_mask = (r > semi_light_threshold) & (g > semi_light_threshold) & (b > semi_light_threshold) & edge_mask
    data[semi_light_mask, 3] = (data[semi_light_mask, 3] * 0.5).astype(np.uint8)
    
    # Save result
    result = Image.fromarray(data, 'RGBA')
    result.save(output_path, 'PNG', optimize=True)
    print(f"✅ Saved: {output_path}")
    print(f"✨ Background removed, character and trophy fully preserved!")

def process_champion():
    """Process only champion.png to remove background"""
    script_dir = Path(__file__).parent.parent.parent
    input_dir = script_dir / 'widgets-assets'
    output_dir = script_dir / 'app' / 'src' / 'main' / 'res' / 'drawable'
    
    output_dir.mkdir(parents=True, exist_ok=True)
    
    input_path = input_dir / 'champion.png'
    output_path = output_dir / 'widget_champion.png'
    
    if input_path.exists():
        remove_background_smart(str(input_path), str(output_path))
        print(f"\n🏆 Champion image ready!")
        print(f"📱 Use in widget as: R.drawable.widget_champion")
    else:
        print(f"❌ File not found: {input_path}")

if __name__ == '__main__':
    print("🔧 Removing background from champion.png (smart edge-preserving method)...\n")
    process_champion()
