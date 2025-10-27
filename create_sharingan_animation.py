"""
Mangekyo Sharingan Animation Generator
Creates a professional rotating Sharingan eye animation and converts it to Lottie JSON
Optimized for mobile app usage
"""

import numpy as np
import matplotlib.pyplot as plt
from matplotlib.patches import Circle, Wedge, FancyBboxPatch
from matplotlib.collections import PatchCollection
import json
from PIL import Image, ImageDraw
import io
import base64

class SharinganAnimator:
    def __init__(self, size=512, frames=60):
        self.size = size
        self.frames = frames
        self.center = (size // 2, size // 2)
        
    def create_frame(self, rotation_angle):
        """Create a single frame of the Sharingan animation"""
        # Create figure with transparent background
        fig, ax = plt.subplots(1, 1, figsize=(5, 5), dpi=self.size//5)
        ax.set_xlim(0, self.size)
        ax.set_ylim(0, self.size)
        ax.set_aspect('equal')
        ax.axis('off')
        fig.patch.set_alpha(0)
        ax.patch.set_alpha(0)
        
        cx, cy = self.center
        
        # Outer red circle (iris)
        outer_circle = Circle((cx, cy), 200, color='#C41E3A', alpha=1.0, zorder=1)
        ax.add_patch(outer_circle)
        
        # Inner gradient circles for depth
        for i, radius in enumerate([180, 160, 140]):
            alpha = 0.3 - i * 0.1
            circle = Circle((cx, cy), radius, color='#8B0000', alpha=alpha, zorder=2)
            ax.add_patch(circle)
        
        # Create the three tomoe (comma shapes)
        num_tomoe = 3
        tomoe_distance = 120
        
        for i in range(num_tomoe):
            angle = rotation_angle + i * (360 / num_tomoe)
            self.draw_tomoe(ax, cx, cy, angle, tomoe_distance)
        
        # Center pupil with highlight
        pupil = Circle((cx, cy), 40, color='#000000', zorder=10)
        ax.add_patch(pupil)
        
        # Pupil highlight for realism
        highlight = Circle((cx - 10, cy + 10), 12, color='#FFFFFF', alpha=0.6, zorder=11)
        ax.add_patch(highlight)
        
        # Outer black ring
        outer_ring = Circle((cx, cy), 220, fill=False, edgecolor='#000000', linewidth=8, zorder=12)
        ax.add_patch(outer_ring)
        
        # Inner ring around tomoe area
        inner_ring = Circle((cx, cy), 100, fill=False, edgecolor='#8B0000', linewidth=3, zorder=9)
        ax.add_patch(inner_ring)
        
        # Convert to image
        buf = io.BytesIO()
        plt.savefig(buf, format='png', transparent=True, bbox_inches='tight', pad_inches=0)
        buf.seek(0)
        img = Image.open(buf)
        plt.close(fig)
        
        return img
    
    def draw_tomoe(self, ax, cx, cy, angle, distance):
        """Draw a single tomoe (comma shape)"""
        # Convert angle to radians
        rad = np.radians(angle)
        
        # Calculate tomoe center position
        tx = cx + distance * np.cos(rad)
        ty = cy + distance * np.sin(rad)
        
        # Main tomoe body (circle)
        tomoe_body = Circle((tx, ty), 35, color='#000000', zorder=5)
        ax.add_patch(tomoe_body)
        
        # Tomoe tail (elongated wedge effect)
        # Create tail using multiple circles for smooth curve
        for j in range(5):
            tail_dist = 20 + j * 8
            tail_angle = angle + 180 + j * 15
            tail_rad = np.radians(tail_angle)
            tail_x = tx + tail_dist * np.cos(tail_rad)
            tail_y = ty + tail_dist * np.sin(tail_rad)
            tail_size = 30 - j * 4
            
            tail_circle = Circle((tail_x, tail_y), tail_size, color='#000000', 
                               alpha=1.0 - j * 0.15, zorder=4)
            ax.add_patch(tail_circle)
        
        # Inner highlight on tomoe
        highlight = Circle((tx + 5, ty + 5), 10, color='#1a1a1a', alpha=0.8, zorder=6)
        ax.add_patch(highlight)
    
    def generate_animation_frames(self):
        """Generate all animation frames"""
        frames = []
        print(f"Generating {self.frames} frames...")
        
        for i in range(self.frames):
            rotation = (i / self.frames) * 360
            print(f"Frame {i+1}/{self.frames} - Rotation: {rotation:.1f}°")
            frame = self.create_frame(rotation)
            frames.append(frame)
        
        return frames
    
    def frames_to_lottie(self, frames):
        """Convert frames to Lottie JSON format (optimized)"""
        print("\nConverting to Lottie JSON...")
        
        # Simplified Lottie structure for rotation animation
        lottie_data = {
            "v": "5.7.4",  # Lottie version
            "fr": 30,  # Frame rate
            "ip": 0,  # In point
            "op": self.frames,  # Out point
            "w": self.size,
            "h": self.size,
            "nm": "Mangekyo Sharingan",
            "ddd": 0,
            "assets": [],
            "layers": [
                {
                    "ddd": 0,
                    "ind": 1,
                    "ty": 4,  # Shape layer
                    "nm": "Outer Ring",
                    "sr": 1,
                    "ks": {
                        "o": {"a": 0, "k": 100},
                        "r": {"a": 0, "k": 0},
                        "p": {"a": 0, "k": [self.size/2, self.size/2, 0]},
                        "a": {"a": 0, "k": [0, 0, 0]},
                        "s": {"a": 0, "k": [100, 100, 100]}
                    },
                    "ao": 0,
                    "shapes": [
                        {
                            "ty": "gr",
                            "it": [
                                {
                                    "ty": "el",
                                    "p": {"a": 0, "k": [0, 0]},
                                    "s": {"a": 0, "k": [440, 440]}
                                },
                                {
                                    "ty": "st",
                                    "c": {"a": 0, "k": [0, 0, 0, 1]},
                                    "o": {"a": 0, "k": 100},
                                    "w": {"a": 0, "k": 8}
                                },
                                {
                                    "ty": "fl",
                                    "c": {"a": 0, "k": [0.768, 0.118, 0.227, 1]},  # Red
                                    "o": {"a": 0, "k": 100}
                                },
                                {
                                    "ty": "tr",
                                    "p": {"a": 0, "k": [0, 0]},
                                    "a": {"a": 0, "k": [0, 0]},
                                    "s": {"a": 0, "k": [100, 100]},
                                    "r": {"a": 0, "k": 0},
                                    "o": {"a": 0, "k": 100}
                                }
                            ],
                            "nm": "Iris",
                            "bm": 0
                        }
                    ],
                    "ip": 0,
                    "op": self.frames,
                    "st": 0,
                    "bm": 0
                },
                {
                    "ddd": 0,
                    "ind": 2,
                    "ty": 4,
                    "nm": "Rotating Tomoe",
                    "sr": 1,
                    "ks": {
                        "o": {"a": 0, "k": 100},
                        "r": {
                            "a": 1,
                            "k": [
                                {"i": {"x": [0.667], "y": [1]}, "o": {"x": [0.333], "y": [0]}, 
                                 "t": 0, "s": [0]},
                                {"t": self.frames, "s": [360]}
                            ]
                        },
                        "p": {"a": 0, "k": [self.size/2, self.size/2, 0]},
                        "a": {"a": 0, "k": [0, 0, 0]},
                        "s": {"a": 0, "k": [100, 100, 100]}
                    },
                    "ao": 0,
                    "shapes": self.create_tomoe_shapes(),
                    "ip": 0,
                    "op": self.frames,
                    "st": 0,
                    "bm": 0
                },
                {
                    "ddd": 0,
                    "ind": 3,
                    "ty": 4,
                    "nm": "Pupil",
                    "sr": 1,
                    "ks": {
                        "o": {"a": 0, "k": 100},
                        "r": {"a": 0, "k": 0},
                        "p": {"a": 0, "k": [self.size/2, self.size/2, 0]},
                        "a": {"a": 0, "k": [0, 0, 0]},
                        "s": {"a": 0, "k": [100, 100, 100]}
                    },
                    "ao": 0,
                    "shapes": [
                        {
                            "ty": "gr",
                            "it": [
                                {
                                    "ty": "el",
                                    "p": {"a": 0, "k": [0, 0]},
                                    "s": {"a": 0, "k": [80, 80]}
                                },
                                {
                                    "ty": "fl",
                                    "c": {"a": 0, "k": [0, 0, 0, 1]},
                                    "o": {"a": 0, "k": 100}
                                },
                                {
                                    "ty": "tr",
                                    "p": {"a": 0, "k": [0, 0]},
                                    "a": {"a": 0, "k": [0, 0]},
                                    "s": {"a": 0, "k": [100, 100]},
                                    "r": {"a": 0, "k": 0},
                                    "o": {"a": 0, "k": 100}
                                }
                            ],
                            "nm": "Pupil Circle",
                            "bm": 0
                        }
                    ],
                    "ip": 0,
                    "op": self.frames,
                    "st": 0,
                    "bm": 0
                }
            ],
            "markers": []
        }
        
        return lottie_data
    
    def create_tomoe_shapes(self):
        """Create tomoe shapes for Lottie"""
        shapes = []
        
        for i in range(3):
            angle = i * 120
            rad = np.radians(angle)
            distance = 120
            
            # Position for tomoe
            tx = distance * np.cos(rad)
            ty = distance * np.sin(rad)
            
            # Create tomoe group
            tomoe_group = {
                "ty": "gr",
                "it": [
                    {
                        "ty": "el",
                        "p": {"a": 0, "k": [tx, ty]},
                        "s": {"a": 0, "k": [70, 70]}
                    },
                    {
                        "ty": "fl",
                        "c": {"a": 0, "k": [0, 0, 0, 1]},
                        "o": {"a": 0, "k": 100}
                    },
                    {
                        "ty": "tr",
                        "p": {"a": 0, "k": [0, 0]},
                        "a": {"a": 0, "k": [0, 0]},
                        "s": {"a": 0, "k": [100, 100]},
                        "r": {"a": 0, "k": angle},
                        "o": {"a": 0, "k": 100}
                    }
                ],
                "nm": f"Tomoe {i+1}",
                "bm": 0
            }
            shapes.append(tomoe_group)
        
        return shapes
    
    def save_lottie(self, lottie_data, filename):
        """Save Lottie JSON to file (optimized)"""
        print(f"\nSaving optimized Lottie JSON to {filename}...")
        
        with open(filename, 'w') as f:
            json.dump(lottie_data, f, separators=(',', ':'))  # Compact JSON
        
        # Get file size
        import os
        size_kb = os.path.getsize(filename) / 1024
        print(f"✓ Lottie JSON saved! Size: {size_kb:.2f} KB")
    
    def save_preview_gif(self, frames, filename):
        """Save animation as GIF for preview"""
        print(f"\nSaving preview GIF to {filename}...")
        
        # Resize frames for smaller GIF
        resized_frames = []
        preview_size = 256
        for frame in frames:
            resized = frame.resize((preview_size, preview_size), Image.LANCZOS)
            resized_frames.append(resized)
        
        # Save as GIF
        resized_frames[0].save(
            filename,
            save_all=True,
            append_images=resized_frames[1:],
            duration=1000//30,  # 30 FPS
            loop=0,
            optimize=True
        )
        
        import os
        size_kb = os.path.getsize(filename) / 1024
        print(f"✓ Preview GIF saved! Size: {size_kb:.2f} KB")


def main():
    print("=" * 60)
    print("🔥 MANGEKYO SHARINGAN ANIMATION GENERATOR 🔥")
    print("=" * 60)
    print()
    
    # Create animator
    animator = SharinganAnimator(size=512, frames=60)
    
    # Generate frames
    frames = animator.generate_animation_frames()
    
    # Save preview GIF
    animator.save_preview_gif(frames, 'sharingan_preview.gif')
    
    # Convert to Lottie
    lottie_data = animator.frames_to_lottie(frames)
    
    # Save Lottie JSON
    animator.save_lottie(lottie_data, 'sharingan_animation.json')
    
    print("\n" + "=" * 60)
    print("✅ ANIMATION GENERATION COMPLETE!")
    print("=" * 60)
    print("\nGenerated files:")
    print("  📄 sharingan_animation.json - Optimized Lottie JSON for your app")
    print("  🎬 sharingan_preview.gif - Preview animation")
    print("\nNext steps:")
    print("  1. Copy sharingan_animation.json to your app's assets folder")
    print("  2. Use Lottie library to load and play the animation")
    print("  3. Example: app/src/main/assets/animations/sharingan.json")
    print()


if __name__ == "__main__":
    main()
