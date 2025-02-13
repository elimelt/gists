import os
import json
import logging
from typing import Dict, Optional, List, Any, Tuple
from dataclasses import dataclass
from enum import Enum
import re
from ollama import chat
from pydantic import BaseModel, Field, validator

logging.basicConfig(
    level=logging.INFO, format="%(asctime)s - %(name)s - %(levelname)s - %(message)s"
)
logger = logging.getLogger(__name__)


class DocumentCategory(str, Enum):
    DISTRIBUTED_SYSTEMS = "Distributed Systems"
    NATURAL_LANGUAGE_PROCESSING = "Natural Language Processing"
    HARDWARE = "Hardware"
    ALGORITHMS = "Algorithms"
    PERFORMANCE_ENGINEERING = "Performance Engineering"
    NETWORKS = "Networks"
    TEACHING = "Teaching"
    MACHINE_LEARNING = "Machine Learning"
    LINEAR_ALGEBRA = "Linear Algebra"
    OPERATING_SYSTEMS = "Operating Systems"
    COMPUTER_SCIENCE = "Computer Science"
    DATABASE_DESIGN = "Database Design"
    DATABASE_SYSTEMS = "Database Systems"
    GRAPH_THEORY = "Graph Theory"
    MATHEMATICS = "Mathematics"
    OPERATIONS_RESEARCH = "Operations Research"
    SOFTWARE_ENGINEERING = "Software Engineering"
    SYSTEM_DESIGN = "System Design"
    RESEARCH = "Research"
    SYSTEMS = "Systems"
    OTHER = "Other"


class TaggingModel(BaseModel):
    """Enhanced model for document tags with validation"""

    title: str = Field(..., min_length=1, max_length=200)
    category: str = Field(..., min_length=1, max_length=50)
    tags: str = Field(..., min_length=1)
    description: str = Field(..., min_length=10, max_length=1000)

    @validator("tags")
    def validate_tags(cls, v):
        """Validate tags format"""
        tags = [tag.strip() for tag in v.split(",")]
        return v

    @validator("category")
    def validate_category(cls, v):
        """Validate category is in predefined list"""
        try:
            return DocumentCategory(v).value
        except ValueError:
            print(f"Invalid category: {v}")
            return DocumentCategory.OTHER.value


class DocumentTagger:
    def __init__(self, model_name: str = "llama3.2", max_retries: int = 3):
        self.model_name = model_name
        self.max_retries = max_retries
        self.json_schema = TaggingModel.model_json_schema()

    def _create_title_prompt(self, document_content: str) -> str:
        return f"""<task>Extract a technical document title</task>

<context>
{document_content[:1000]}  # Only first 1000 chars needed for title
</context>

<requirements>
- Maximum length: 200 characters
- If main heading exists (# or ##), use that directly
- No markdown formatting in output
- Must be specific and technical
- Focus on core subject matter
</requirements>

<output_format>
Title only, no explanations or additional text
</output_format>

<examples>
Input: # Building Distributed Systems with Kafka
Output: Building Distributed Systems with Kafka

Input: This paper discusses optimization techniques...
Output: Optimization Techniques for Large-Scale Systems

Bad output: "Title: Database Sharding Patterns"
Bad output: # Database Sharding Patterns
</examples>"""

    def _create_category_prompt(self, document_content: str, title: str) -> str:
        categories = "\n".join([f"- {cat.value}" for cat in DocumentCategory])
        return f"""<task>Categorize a technical document</task>

<context>
Title: {title}
First 1000 chars: {document_content[:1000]}
</context>

<valid_categories>
{categories}
</valid_categories>

<requirements>
- Select exactly ONE category
- Match category name exactly
- Use Other only if no category fits
</requirements>

<output_format>
Category name only, no explanations or additional text
</output_format>

<examples>
Good: Database Systems
Bad: "Category: Database Systems"
Bad: This belongs in Database Systems
</examples>"""

    def _create_tags_prompt(self, document_content: str, title: str, category: str) -> str:
        return f"""<task>Generate technical tags</task>

<context>
Title: {title}
Category: {category}
First 1500 chars: {document_content[:1500]}
</context>

<requirements>
- 3-7 tags total
- Comma-separated format
- Specific technical terms only
- Mix of high-level and specific concepts
- No markdown or formatting
</requirements>

<output_format>
tag1, tag2, tag3
No explanations or additional text
</output_format>

<examples>
Good: distributed systems, fault tolerance, consensus algorithms
Bad: "Tags: databases, sql, indexing"
Bad: 1. databases 2. sql 3. indexing
</examples>"""

    def _create_description_prompt(self, document_content: str, title: str, category: str, tags: str) -> str:
        headers = "\n".join([line for line in document_content.split("\n") if line.startswith("#")])

        return f"""<task>Write technical document description</task>

<context>
Title: {title}
Category: {category}
Tags: {tags}
Document headers:
{headers}
</context>

<requirements>
- Maximum 3 sentences
- No markdown formatting
- Focus on technical content
- Be specific about concepts covered
- Maximum length: 1000 characters
- Don't start with "This document" or similar
</requirements>

<output_format>
Plain text description only
No explanations or additional text
</output_format>

<examples>
Good: Explores distributed consensus algorithms with focus on Raft implementation. Covers leader election, log replication, and safety proofs.
Bad: "Description: This document covers distributed systems..."
Bad: # Overview of consensus algorithms
</examples>"""

    def _get_single_field(self, prompt: str, attempt_num: int) -> str:
        """Get a single field from the model with error handling"""
        try:
            response = chat(
                model=self.model_name,
                messages=[
                    {
                        "role": "system",
                        "content": "You are a precise technical document analyzer. Your task is to output ONLY the requested information without any additional text, formatting, or explanations.",
                    },
                    {"role": "user", "content": prompt},
                ],
                options={"num_ctx": 2048, "max_tokens": 200},
            )
            return response["message"]["content"].strip()
        except Exception as e:
            logger.warning(f"Attempt {attempt_num + 1} failed: {str(e)}")
            raise

    def get_tags(self, document_content: str) -> Optional[Dict[str, Any]]:
        """Get tags for a document with retry logic and validation"""
        for attempt in range(self.max_retries):
            try:
                title = self._get_single_field(
                    self._create_title_prompt(document_content), attempt
                )

                category = self._get_single_field(
                    self._create_category_prompt(document_content, title), attempt
                )

                tags = self._get_single_field(
                    self._create_tags_prompt(document_content, title, category), attempt
                )

                description = self._get_single_field(
                    self._create_description_prompt(
                        document_content, title, category, tags
                    ),
                    attempt,
                )

                # Combine into final format
                tags_dict = {
                    "title": title,
                    "category": category,
                    "tags": tags,
                    "description": description,
                }

                # Validate against model
                validated_tags = TaggingModel(**tags_dict)
                return validated_tags.model_dump()

            except Exception as e:
                logger.warning(f"Attempt {attempt + 1} failed: {str(e)}")
                if attempt == self.max_retries - 1:
                    logger.error(f"All attempts failed to generate valid tags")
                    raise ValueError(
                        f"Failed to generate valid tags after {self.max_retries} attempts"
                    )