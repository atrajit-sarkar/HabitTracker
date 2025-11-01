"""
Remove black/dark background from widget PNG images using AI
"""
import sys
import os
from pathlib import Path
from PIL import Image
import numpy as np

def remove_dark_background(input_path, output_path, threshold=50):
    """
    Remove dark/black background from PNG image
    
    Args:
        input_path: Path to input PNG
        output_path: Path to output PNG
        threshold: RGB value below which pixels are considered background (0-255)
    """
    print(f"Processing: {input_path}")
    
    # Open image
    img = Image.open(input_path).convert('RGBA')
    data = np.array(img)
    
    r, g, b, a = data[:,:,0], data[:,:,1], data[:,:,2], data[:,:,3]
    
    # Detect dark pixels (likely background)
    dark_mask = (r < threshold) & (g < threshold) & (b < threshold)
    
    # Calculate gradient to detect edges (preserve anti-aliasing)
    gray = (r.astype(float) + g.astype(float) + b.astype(float)) / 3
    gradient_x = np.abs(np.diff(gray, axis=1, prepend=gray[:,0:1]))
    gradient_y = np.abs(np.diff(gray, axis=0, prepend=gray[0:1,:]))
    gradient = np.sqrt(gradient_x**2 + gradient_y**2)
    
    # Preserve edges (anti-aliased pixels)
    edge_mask = gradient > 10
    
    # Remove dark pixels that are not near edges
    removal_mask = dark_mask & ~edge_mask
    
    # Set alpha to 0 for background pixels
    data[removal_mask, 3] = 0
    
    # Also reduce alpha for semi-dark pixels (smooth transition)
    semi_dark_mask = (r < threshold * 2) & (g < threshold * 2) & (b < threshold * 2) & ~removal_mask
    data[semi_dark_mask, 3] = (data[semi_dark_mask, 3] * 0.3).astype(np.uint8)
    
    # Save result
    result = Image.fromarray(data, 'RGBA')
    result.save(output_path, 'PNG', optimize=True)
    print(f"Saved: {output_path}")

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
    
    for filename in png_files:
        input_path = input_dir / filename
        if input_path.exists():
            # Convert filename to Android resource naming (lowercase, no hyphens)
            output_name = f"widget_{filename.replace('-', '_')}"
            output_path = output_dir / output_name
            
            try:
                remove_dark_background(input_path, output_path, threshold=50)
            except Exception as e:
                print(f"Error processing {filename}: {e}")
        else:
            print(f"File not found: {input_path}")

if __name__ == '__main__':
    print("🎨 Removing backgrounds from widget assets...")
    process_widget_assets()
    print("✅ Done!")
